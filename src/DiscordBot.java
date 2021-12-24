import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.Timer;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

public class DiscordBot extends WebSocketAdapter implements ActionListener {
	String channelID;
	String token;
	String name;
	private String sessionID;
	WebSocket socket;
	private Timer heartbeatTimer;
	private Integer sequenceCode;
	private boolean reconnect;
	private int heartbeatInterval;
	boolean rpsMode = false;
	int playerScore = 0;
	int cpuScore = 0;
	

	public DiscordBot(String c, String t, String n) {
		channelID = c;
		token = t;
		name = n;
		initializeSocketConnection();
	}
	
	private void initializeSocketConnection() {
		WebSocketFactory factory = new WebSocketFactory();
		try {
			socket = factory.createSocket(getGateway());
			socket.addListener(this);
			socket.connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (WebSocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {		
		BufferedReader br = new BufferedReader(new FileReader("/Users/league/desktop/token.txt"));
		//BufferedReader br = new BufferedReader(new FileReader("/C:/Users/tchen/Desktop/token.txt"));
		String name = br.readLine();
		String token = br.readLine();
		String channelID = br.readLine();
		br.close();
		DiscordBot bot = new DiscordBot(channelID, token, name);
		
	}
	
	void sendMessage(String m) {
		String message = "{\"content\":\""+m+"\"}";
		try {
			URL url = new URL("https://discord.com/api/channels/890750802430423053/messages");
			HttpsURLConnection connect = (HttpsURLConnection) url.openConnection();
			connect.setDoOutput(true);
			connect.setRequestMethod("POST");
			connect.setRequestProperty("Authorization", "Bot " + token);
			connect.setRequestProperty("Content-Type", "application/json");
			connect.setRequestProperty("User-Agent", "");
			OutputStream out = connect.getOutputStream();
			out.write(message.getBytes());
			out.flush();
			out.close();
			connect.getInputStream().close();
			connect.disconnect();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	String getGateway() {
		try {
			URL url = new URL("https://discord.com/api/gateway/bot");
			HttpsURLConnection connect = (HttpsURLConnection) url.openConnection();
			connect.setRequestMethod("GET");
			connect.setRequestProperty("Authorization", "Bot " + token);
			connect.setRequestProperty("Content-Type", "application/json");
			connect.setRequestProperty("User-Agent", "");
			InputStream in = connect.getInputStream();
			String data  = "";
			int v = in.read();
			while(v != -1) {
				data += (char) v;
				v = in.read();
			}
			JsonObject obj = getJsonObjectFromString(data);
			return obj.getString("url");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	public JsonObject getJsonObjectFromString(String s) {
		JsonReader reader = Json.createReader(new StringReader(s));
		JsonObject obj = reader.readObject();
		reader.close();
		return obj;
	}
	
	@Override
	public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
		// TODO Auto-generated method stub
		System.out.println("connected");
	}
	
	@Override
	public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
			boolean closedByServer) {
		// TODO Auto-generated method stub
		System.out.println("disconnected");
		if(closedByServer) {
			reconnect = true;
			initializeSocketConnection();
		}
	}
	
	@Override
	public void onError(WebSocket websocket, WebSocketException cause) {
		// TODO Auto-generated method stub
		System.out.println(cause);
	}
	
	@Override
	public void onFrame(WebSocket websocket, WebSocketFrame frame) {
		// TODO Auto-generated method stub
		System.out.println(frame);
		String payload = frame.getPayloadText();
		JsonObject obj = getJsonObjectFromString(payload);
		try {
			sequenceCode = obj.getInt("s");
		} catch(Exception e) {
			sequenceCode = null;
		}
		int op = obj.getInt("op");
		if(op == 10) {
			JsonObject d = obj.getJsonObject("d");
			heartbeatInterval = d.getInt("heartbeat_interval");
			String auth = "";
			if(reconnect) {
				auth = "{\n" + 
						"  \"op\": 6,\n" + 
						"  \"d\": {\n" + 
						"    \"token\": \""+token+"\",\n" + 
						"    \"session_id\": \""+ sessionID + "\",\n" + 
						"    \"seq\": " + sequenceCode + "\n" +  
						"  }\n" + 
						"}";
				reconnect = false;
			}
			else {
			auth = "{\n" + 
					"  \"op\": 2,\n" + 
					"  \"d\": {\n" + 
					"    \"token\": \""+token+"\",\n" + 
					"    \"intents\": 513,\n" + 
					"    \"properties\": {\n" + 
					"      \"$os\": \"linux\",\n" + 
					"      \"$browser\": \"my_library\",\n" + 
					"      \"$device\": \"my_library\"\n" + 
					"    }\n" + 
					"  }\n" + 
					"}"
					+ "";
			}
			socket.sendText(auth);
		} else if(op == 0) {
			String type = obj.getString("t");
			if(type.equals("MESSAGE_CREATE")) {
				JsonObject d = obj.getJsonObject("d");
				String text = d.getString("content");
				JsonObject author = d.getJsonObject("author");
				String user = author.getString("username");
				if(!user.equals(name)) {
					messageReceived(text, user);
				}
			} else if(type.equals("READY")) {
				JsonObject d = obj.getJsonObject("d");
				sessionID = d.getString("session_id");
				heartbeatTimer = new Timer(heartbeatInterval, this);
				sendHeartbeat();
				heartbeatTimer.start();
			}
		} else if(op == 1) {
			sendHeartbeat();
		}
	}
	
	void messageReceived(String message, String user) {
		String[] words = message.split(" ");
		String trigger1 = "!whoami";
		String trigger2 = "!rps";
		String trigger3 = "!showlength";
		if(message.equals(trigger1)) {
			sendMessage("You are " + user + ".");
		}
		if(message.equals(trigger2)) {
			rpsMode = true;
			sendMessage("Use the command !play (object) to play. Send !score to view player and bot scores."
					+ " When you're done, send the command !end.");
		}
		if(words[0].equals(trigger3)) {
			String show = "";
			for(int i = 1; i < words.length; i++) {
				if(i == 1) {
					show = show + words[1];
				}
				else {
					show = show + " " + words[i];
				}
			}
			try {
				TVApp(show);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(rpsMode) {
			String playTrigger = "!play";
			String endTrigger = "!end";
			String scoreTrigger = "!score";
			if(words[0].equals(playTrigger)) {
				int comChoice = new Random().nextInt(3);
				if(words[1].equalsIgnoreCase("Rock")) {
					if(comChoice == 0) {
						sendMessage("You chose Rock. The Bot chose Rock. Tie!");
					} else if(comChoice == 1) {
						sendMessage("You chose Rock. The Bot chose Paper. You lose!");
						cpuScore++;
					} else if(comChoice == 2) {
						sendMessage("You chose Rock. The Bot chose Scissors. You win!");
						playerScore++;
					}
				} else if(words[1].equalsIgnoreCase("Paper")) {
					if(comChoice == 0) {
						sendMessage("You chose Paper. The Bot chose Rock. You win!");
						playerScore++;
					} else if(comChoice == 1) {
						sendMessage("You chose Paper. The Bot chose Paper. Tie!");
					} else if(comChoice == 2) {
						sendMessage("You chose Paper. The Bot chose Scissors. You lose!");
						cpuScore++;
					}
				} else if(words[1].equalsIgnoreCase("Scissors")) {
					if(comChoice == 0) {
						sendMessage("You chose Scissors. The Bot chose Rock. You lose!");
						cpuScore++;
					} else if(comChoice == 1) {
						sendMessage("You chose Scissors. The Bot chose Paper. You win!");
						playerScore++;
					} else if(comChoice == 2) {
						sendMessage("You chose Scissors. The Bot chose Scissors. Tie!");
					}
				}
			}
			if (words[0].equals(endTrigger)) {
				sendMessage("Game ended.");
				playerScore = 0;
				cpuScore = 0;
				rpsMode = false;
			}
			if (words[0].equals(scoreTrigger)) {
				sendMessage("Player score: " + playerScore);
				sendMessage("Bot score: "+ cpuScore);
			}
		}
	}
	
	void TVApp(String show) throws IOException {
		int id = getShowID(show);
		URL site = new URL("https://api.tvmaze.com/shows/"+id+"/seasons");
		HttpURLConnection connection = (HttpURLConnection) site.openConnection();
		InputStream input = connection.getInputStream();
		JsonReader reader = Json.createReader(input);
		JsonArray arr = reader.readArray();
		int totalSeasons = arr.size();
		for(int i = 0; i < totalSeasons; i++) {
			JsonObject obj = arr.getJsonObject(i);
			sendMessage("Season "+(i+1)+": "+obj.getInt("episodeOrder")+" Episodes");
		}
	}
	
	static int getShowID(String show) throws IOException {
		URL site = new URL("https://api.tvmaze.com/singlesearch/shows?q="+show);
		HttpURLConnection connection = (HttpURLConnection) site.openConnection();
		InputStream input = connection.getInputStream();
		JsonReader reader = Json.createReader(input);
		JsonObject object = reader.readObject();
		input.close();
		int id = object.getInt("id");
		return id;
	}
	
	private void sendHeartbeat() {
		String outData = "{\r\n" +
				"	\"op\": 1,\r\n" +
				"	\"d\": " + sequenceCode + "\r\n" +
				"}";
		socket.sendText(outData);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		sendHeartbeat();
	}
}

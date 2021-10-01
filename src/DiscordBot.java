import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

public class DiscordBot extends WebSocketAdapter {
	String channelID;
	String token;
	String name;
	WebSocket socket;

	public DiscordBot(String c, String t, String n) {
		channelID = c;
		token = t;
		name = n;
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
	}
	
	@Override
	public void onError(WebSocket websocket, WebSocketException cause) {
		// TODO Auto-generated method stub
		System.out.println(cause);
	}
	
	@Override
	public void onFrame(WebSocket websocket, WebSocketFrame frame) {
		// TODO Auto-generated method stub
		String payload = frame.getPayloadText();
		JsonObject obj = getJsonObjectFromString(payload);
		int op = obj.getInt("op");
		if(op == 10) {
			String auth = "{\n" + 
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
			socket.sendText(auth);
		} else if(op == 0) {
			String type = obj.getString("t");
			if(type.equals("MESSAGE_CREATE")) {
				JsonObject d = obj.getJsonObject("d");
				String text = d.getString("content");
				JsonObject author = d.getJsonObject("author");
				String user = author.getString("username");
				if(!user.contentEquals(name)) {
					messageReceived(text, user);
				}
			}
		}
		System.out.println(frame);
	}
	
	private void messageReceived(String message, String user) {
		String trigger = "!whoami";
		if(message.substring(0, trigger.length()).contentEquals(trigger)) {
			sendMessage("You are " + user + ".");
		}
	}
	//Just in case for next week: bit.ly/3ioFtUZ
}

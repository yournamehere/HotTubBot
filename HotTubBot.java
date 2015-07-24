import org.jibble.pircbot.*;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.*;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.json.*;

public class HotTubBot extends PircBot {

	static final String INVOKE_HOTTUB_COMMAND = ".hottub";
	static final String BOT_NAME = "HotTub";
//	static final String SERVER = "127.0.0.1";
	static final String SERVER = "irc.synirc.net";
	static final String CHANNEL = "#mtgoon";

	UndirectedGraph<String, DefaultEdge> namesGraph;
	Map<String,String> realNames;

	public HotTubBot() throws Exception{
		this.setName(BOT_NAME);
		initNames();
	}
	private void initNames() throws Exception{

		Map<String,ArrayList<String>> dict = new HashMap<String,ArrayList<String>>();
		namesGraph = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
		realNames = new HashMap<String,String>();

		String rawtext = new String(Files.readAllBytes(Paths.get("AllCards.json")));
		JSONObject cards = new JSONObject(rawtext);
		Iterator<String> names = cards.keys();
		while(names.hasNext()){
			String name = names.next();
			JSONObject card = cards.getJSONObject(name);

			String layout = card.getString("layout");
			if(layout.equals("vanguard")) continue;
			if(layout.equals("scheme")) continue;
			if(layout.equals("plane")) continue;
			if(layout.equals("token")) continue;
			if(layout.equals("phenomenon")) continue;

			realNames.put(name.toLowerCase(), name);
			name = name.toLowerCase();
			namesGraph.addVertex(name);
			String[] words = name.split(" ");
			for(String word : words){
				word = word.toLowerCase();
				if (word.equals("of") || word.equals("the") || word.equals("and") || word.equals("to") || word.equals("from") || word.equals("in") || word.equals("for") || word.equals("at") || word.equals("a") || word.equals("into") || word.equals("with")){
					continue;
				}
				word = word.replace("\"", "").replace("'s", "").replace("!", "").replace("?", "").replace("s'", "s").replace(",", "").replace("(", "").replace(")","");
				if(dict.containsKey(word)){

					for(String neighbor : dict.get(word)){
						if (neighbor.equals(name)) continue;
						namesGraph.addEdge(name, neighbor);
					}
					dict.get(word).add(name);
				}
				else{
					ArrayList<String> newlist = new ArrayList<String>();
					newlist.add(name);
					dict.put(word, newlist);
				}
			}
		}

	}
	private void printUsage(String channel, String sender){
				sendMessage(channel, sender + ": Usage: " + INVOKE_HOTTUB_COMMAND + " card1 > card2 , or " + INVOKE_HOTTUB_COMMAND + " card");
	}
	public void onDisconnect(){
		int waitTime  = 8;
		for(int attempts = 0; attempts < 10; attempts++){
			try{
				TimeUnit.SECONDS.sleep(waitTime);
				reconnect();
				return;
			}
			catch (Exception e){
				e.printStackTrace();
				waitTime*=2;
				System.out.println("Waiting "+waitTime);
			}
		}
	}

	public void onPrivateMessage(String sender, String login, String hostname, String message){
		if (! message.toLowerCase().startsWith(INVOKE_HOTTUB_COMMAND)){
			printUsage(sender, sender);
		}
		else{
			onMessage(sender, sender, login, hostname, message);
		}
	}

	public void onMessage(String channel, String sender, String login, String hostname, String message){

		if (message.toLowerCase().equals(INVOKE_HOTTUB_COMMAND)){
			printUsage(channel, sender);
			return;
		}
		if (message.toLowerCase().startsWith(INVOKE_HOTTUB_COMMAND)){
			message = message.substring(INVOKE_HOTTUB_COMMAND.length());
			String[] cardNames = message.split(">");
			
			if (cardNames.length > 2){
				printUsage(channel, sender);
				return;
			}
			if(cardNames.length == 1){
				String response = adjacentNodes(cardNames[0].trim());
				sendMessage (channel, sender + ":" + response);
				return;
			}
			//else length == 2
			String start = cardNames[0].trim();
			String end =cardNames[1].trim();

			String response = shortestPath(start, end);
			sendMessage(channel, sender + ": " + response);
		}
	}
	private String adjacentNodes(String vertex){
		if (!namesGraph.containsVertex(vertex.toLowerCase())){
			return "Can't find card " + vertex;
		}
		vertex = vertex.toLowerCase();

		StringBuilder sb = new StringBuilder();
		ConnectivityInspector<String,DefaultEdge> connect = new ConnectivityInspector<String,DefaultEdge>(namesGraph);
		Set<String> connectedComponent = connect.connectedSetOf(vertex);
		if(connectedComponent.size() < 10){
			sb.append("belongs to component:");
			for(String member : connectedComponent){
				sb.append(" ");
				sb.append(realNames.get(member));
				sb.append(",");
			}
			sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
		//else in large component
		int degree = namesGraph.degreeOf(vertex);
		Set<DefaultEdge> adjacent = namesGraph.edgesOf(vertex);
		for(DefaultEdge edge : adjacent){
			sb.append(" ");
			String source = namesGraph.getEdgeSource(edge);
			String target = namesGraph.getEdgeTarget(edge); 
			if (source.equals(vertex)){
				sb.append(realNames.get(target));
			}
			else{
				sb.append(realNames.get(source));
			}
			sb.append(",");
		}
		sb.deleteCharAt(sb.length()-1);
		return  realNames.get(vertex) + " has degree " + degree + ": " + sb.toString();

	}
	private String shortestPath(String startpoint, String endpoint){

		String startlower = startpoint.toLowerCase();
		String endlower = endpoint.toLowerCase();

		if (!namesGraph.containsVertex(startpoint.toLowerCase())){
			return "Can't find card " + startpoint;
		}
		if (!namesGraph.containsVertex(endpoint.toLowerCase())){
			return "Can't find card " + endpoint;
		}
		startpoint = startpoint.toLowerCase();
		endpoint = endpoint.toLowerCase();

		List<DefaultEdge> path = DijkstraShortestPath.findPathBetween(namesGraph, startpoint,endpoint);
		if(path == null){
			return "No path exists";
		}
		
		StringBuilder sb = new StringBuilder();
		String previous = startpoint;
		sb.append(realNames.get(startpoint));

		for(DefaultEdge edge : path){
			sb.append(" > ");
			String source = namesGraph.getEdgeSource(edge);
			String target = namesGraph.getEdgeTarget(edge);
			if(source.equals(previous)){
				sb.append(realNames.get(target));
				previous=target;
			}
			else if(target.equals(previous)){
				sb.append(realNames.get(source));
				previous=source;
			}
		}
		sb.append(" ("+path.size()+")");
		return sb.toString();
	}
	public static void main(String[] args) throws Exception{
		HotTubBot bot = new HotTubBot();
		bot.setVerbose(true);
		bot.connect(SERVER);
		bot.joinChannel(CHANNEL);
	}
}
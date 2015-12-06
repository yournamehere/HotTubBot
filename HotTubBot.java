import org.jibble.pircbot.*;

import java.util.*;

import java.util.concurrent.TimeUnit;

public class HotTubBot extends PircBot {

	static final String INVOKE_HOTTUB_COMMAND = ".hottub";
	static final String BOT_NAME = "HotTub";
//	static final String SERVER = "127.0.0.1";
	static final String SERVER = "irc.synirc.net";
	static final String CHANNEL = "#mtgoon";

	CardsGraph cardsGraph;

	public HotTubBot() throws Exception{
		this.setName(BOT_NAME);
		cardsGraph = new CardsGraph();
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
				String response = cardsGraph.adjacentNodes(cardNames[0].trim());
				sendMessage (channel, sender + ": " + response);
				return;
			}
			//else length == 2
			String start = cardNames[0].trim();
			String end =cardNames[1].trim();

			String response = cardsGraph.shortestPath(start, end);
			sendMessage(channel, sender + ": " + response);
		}
	}
	public static void main(String[] args) throws Exception{
		HotTubBot bot = new HotTubBot();
		bot.setVerbose(true);
		bot.connect(SERVER);
		bot.joinChannel(CHANNEL);
	}
}

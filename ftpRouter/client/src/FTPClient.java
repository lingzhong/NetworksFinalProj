import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.PriorityQueue;
import java.util.Collections;

/******************************
 * 
 * zhongli3 (998249177)
 *
 ******************************/

// The network is represented by a graph, that contains nodes and edges
class Node implements Comparable<Node>
{
	public final int name;
	public Edge[] neighbors;
	public double minDistance = Double.POSITIVE_INFINITY;
	public Node previous;     // to keep the path for backtracking
	public Node(int argName) 
	{ 
		name = argName; 
	}

	public int compareTo(Node other)
	{
		return Double.compare(minDistance, other.minDistance);
	}
}

class Edge
{
	public final Node target;
	public final double weight;
	public Edge(Node argTarget, double argWeight)
	{ 
		target = argTarget;
		weight = argWeight; 
	}
}

class Listener implements Runnable {
	Socket socket;
	BufferedReader reader;
	
	public Listener(Socket s, BufferedReader br) {
		this.socket = s;
		this.reader = br;
	}
	
	@Override
	public void run() {
		try {
			while (!socket.isClosed()) {
				int ack = Integer.parseInt(reader.readLine());
				FTPClient.incrementAck(ack);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
}

public class FTPClient {

	static String mode;
	static String host;
	static int port;
	
	static int lastAck = 0;
	static final String CRLF = "\r\n";
	static final int SIZE_OF_HEADER = 4;
	static final int SIZE_OF_DATA = 1000;

	public static void adjacenyToEdges(double[][] matrix, List<Node> v)
	{
		for(int i = 0; i < matrix.length; i++)
		{
			v.get(i).neighbors = new Edge[matrix.length];
			for(int j = 0; j < matrix.length; j++)
			{
				v.get(i).neighbors[j] = new Edge(v.get(j), matrix[i][j]);	
			}
		}
	}
	
	public static void incrementAck(int ack) {
		if (ack != -1 && ack > lastAck) {
			lastAck = ack;
			System.out.println("Received ack " + ack);
		}
	}

	// compute all minDistance from source and
    // populate all destination using dijkstra
	public static void computePaths(Node source)
	{
		source.minDistance = 0;
		PriorityQueue<Node> nodeQ = new PriorityQueue<Node>();
		Node src = source;
		nodeQ.add(src);
		while (!nodeQ.isEmpty()) {
			src = nodeQ.poll();
			for (Edge e : src.neighbors) {
				Node tar = e.target;
				double distanceThroughSource = src.minDistance + e.weight;
				if (distanceThroughSource < tar.minDistance) {
					nodeQ.remove(tar);
					tar.minDistance = distanceThroughSource;
					tar.previous = src;
					nodeQ.add(tar);
				}
			}
		}
	}

	public static List<Integer> getShortestPathTo(Node target)
	{
		List<Integer> path = new ArrayList<Integer>();
		Node cur = target;
		while (cur != null) {
			path.add(cur.name);
			cur = cur.previous;
		}
		Collections.reverse(path);
		return path;
	}

	/**
	 * @param args
	 */

	public static void main(String[] args) {

		if(args.length<=0)
		{
			mode="client";
			host="localhost";
			port=9876;
		}
		else if(args.length==1)
		{
			mode=args[0];
			host="localhost";
			port=9876;
		}
		else if(args.length==3)
		{
			mode=args[0];
			host=args[1];
			port=Integer.parseInt(args[2]);
		}
		else
		{
			System.out.println("improper number of arguments.");
			return;
		}

		try 
		{
			Socket socket=null;
			if(mode.equalsIgnoreCase("client"))
			{
				socket=new Socket(host, port);
			}
			else if(mode.equalsIgnoreCase("server"))
			{
				ServerSocket ss=new ServerSocket(port);
				socket=ss.accept();
			}
			else
			{
				System.out.println("improper type.");
				return;
			}
			System.out.println("Connected to : "+ host+ ":"+socket.getPort());
			
			/* FINDING SHORTEST PATH */
			BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream())); //for reading lines
			DataOutputStream writer=new DataOutputStream(socket.getOutputStream());	//for writing lines.
			
			// grab noNodes
			int noNodes = Integer.parseInt(reader.readLine());
			System.out.println("Server sent " + noNodes + " nodes");
			
			// grab delay matrix
			String matrixString = reader.readLine();
			
			// Create an adjacency matrix after reading from server
			double[][] matrix = new double[noNodes][noNodes];
			
			// Use StringTokenizer to store the values read from the server in matrix
			StringTokenizer st = new StringTokenizer(matrixString);
			for (int i = 0; i < matrix.length; i++) {
				for (int j = 0; j < matrix[0].length; j++) {
					matrix[i][j] = Double.parseDouble(st.nextToken());
				}
			}
			
			// print the delay matrix
			for (int i = 0; i < matrix.length; i++) {
				for (int j = 0; j < matrix[0].length; j++) {
					System.out.print(matrix[i][j] + " ");
				}
				System.out.println();
			}
			
			// The nodes are stored in a list, nodeList
			List<Node> nodeList = new ArrayList<Node>();
			for(int i = 0; i < noNodes; i++){
				nodeList.add(new Node(i));
			}
			
			// Create edges from adjacency matrix
			adjacenyToEdges(matrix, nodeList);
			
			// Finding shortest path from [0] to [noNodes-1]
			Node src = nodeList.get(0);
			Node dst = nodeList.get(noNodes-1);
			computePaths(src);
			List<Integer> shortestPath = getShortestPathTo(dst);
			System.out.print("Total time to reach node " + dst.name + ": " + dst.minDistance + "ms, ");
			System.out.println("Path: " + shortestPath);
			// send path to server
			writer.writeBytes(shortestPath.toString() + CRLF);
			
			/* TRANSFER FILE TO SERVER */
			Scanner scr = new Scanner(System.in);
			System.out.println("Enter the name of the file to transfer to server");
			String fileName = scr.next();
			scr.close();
			
			writer.writeBytes(fileName + CRLF);
			
			// grab file into byte array
			RandomAccessFile f = new RandomAccessFile(fileName, "r");
			byte[] fBytes = new byte[(int) f.length()];
			f.read(fBytes);
			f.close();
			
			// find noPackets
			int noPackets = (int) Math.ceil(fBytes.length/1000.0);
			System.out.println("A total of " + noPackets + " to send.");
			
			// inform server of noPackets
			writer.writeBytes(Integer.toString(noPackets) + CRLF);
			// partition all the bytes into 1000 chunks and append header
			byte[][] packets = new byte[noPackets][];
			for (int i = 0; i < packets.length; i++) {
				byte[] header = ByteBuffer.allocate(SIZE_OF_HEADER).putInt(i+1).array();
				byte[] data = Arrays.copyOfRange(fBytes, i*SIZE_OF_DATA, (int) Math.min((i+1)*SIZE_OF_DATA, fBytes.length));
				packets[i] = new byte[header.length + data.length];
				System.arraycopy(header, 0, packets[i], 0, header.length);
				System.arraycopy(data, 0, packets[i], header.length, data.length);
			}
			
			int timeoutInterval = (int) (2*dst.minDistance + 200);
			int cwnd = 1;
			int packetNumber = 0;
			int ssthresh = Integer.MAX_VALUE;
			int RTTcount = 0;
			
			Thread t = new Thread(new Listener(socket, reader));
			t.start();
			long transferStartTime = System.currentTimeMillis();
			while (packetNumber < noPackets) {
				long packetSentTime = System.currentTimeMillis();
				
				for (int i = packetNumber; i < Math.min(packetNumber + cwnd, noPackets); i++) {
					writer.write(packets[i], 0, packets[i].length);
					System.out.println("Pack number " + (i+1) + " of size " + packets[i].length + " sent");
				}
				
				// polling time to check time out
				int lastPacketSent = Math.min(packetNumber + cwnd, noPackets);
				boolean timedOut = false;
				while (lastAck != lastPacketSent && !timedOut) {
					long curTime = System.currentTimeMillis();
					long timePassed = curTime - packetSentTime;
					if (timePassed > timeoutInterval) {
						timedOut = true;
						System.out.println("Timed out at ack " + lastAck);
					}
				}
				// adjust sliding window protocol parameters based on timedOut
				if (timedOut) {
					ssthresh = cwnd/2;
					System.out.println("updated ssthresh: " + ssthresh);
					cwnd = 1;
					System.out.println("updated cwnd: " + cwnd);
					packetNumber = lastAck;
				} else {
					cwnd = cwnd >= ssthresh ? cwnd+1 : cwnd*2;
					System.out.println("updated cwnd: " + cwnd);
					packetNumber = lastAck;
				}
				RTTcount++;
			}
			long transferEndTime = System.currentTimeMillis();
			System.out.println("Total time to send all packets: " + (transferEndTime - transferStartTime)/1000 + " seconds.");
			System.out.println("Total time in terms of RTT: " + RTTcount + " RTT.");
			System.out.println(lastAck + " out of " + noPackets + " packets have been sent successfully");

			// clean up resources
			socket.close();
			reader.close();
			writer.close();
			t.join();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Exit");
		}
	}

}

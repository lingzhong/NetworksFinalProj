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
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Exit");
		}
	}

}

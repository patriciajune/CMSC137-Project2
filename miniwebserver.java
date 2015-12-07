import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedWriter;
import java.io.FileWriter;


public class miniwebserver extends Thread{
	private int port;

	public miniwebserver(int listen_to_port) {
		this.port = listen_to_port;
	}
	
	@Override
	public void run(){
		ServerSocket serversocket = null;
		
		try {
			System.out.println("Binding to port " + port); 
			serversocket = new ServerSocket(port);  //create socket and bind to port
			System.out.println("Binding successful");//successful
		} catch (IOException e1) {
			System.out.println(e1.getMessage());
		}
		
		while(true){
			//waiting for requests
			try{
				Socket connectionsocket = serversocket.accept(); //waiting for connection
				InetAddress client = connectionsocket.getInetAddress(); //get ip address of client
				
				//check if successful, print
				System.out.println("Server now connected to " + client.getHostName());
				
				BufferedReader input = new BufferedReader(new InputStreamReader(connectionsocket.getInputStream()));  //read http request
				DataOutputStream output = new DataOutputStream(connectionsocket.getOutputStream()); //send response to client
				parser(input,output);
			}
			catch(Exception e){ //error
				System.out.println("Error 2" + e.getMessage());
			}
		}
	}

	private void parser(BufferedReader input, DataOutputStream output) throws IOException {
		String requestline = null;
		String requestedfile = null;
		int requestMethod = 0;
		try{
			requestline = input.readLine(); //read REQUEST URI
			//chop request file using space as delimiter, then get the second string which will be your requested file
			requestedfile = requestline.split("\\s")[1];
			//map for key-value pairs
			Map<String, String> map = new HashMap<String, String>();
			
			if(requestline.startsWith("GET")){  //GET METHOD
				if(requestedfile.contains("?")){ 
					String temp2 = requestedfile.substring(requestedfile.indexOf("?")+1);
					requestedfile = requestedfile.substring((requestedfile.indexOf("/"))+ 1, requestedfile.indexOf("?"));
					
					String[] parameters = temp2.split("&");  //more than one pairs
					for(int i=0;i<parameters.length;i++){
						String[] tokens = parameters[i].split("=");
						map.put(tokens[0], tokens[1]);
					}
				}
				else{ //one requested file
					requestedfile = requestedfile.substring((requestedfile.indexOf("/"))+ 1);
				}
				requestMethod = 1;
			}else if(requestline.startsWith("POST")){ //POST METHOD
				requestMethod = 2;
			}
			System.out.println("\nHeaders:\n");
			//header
			while(input.ready()){
				String s = input.readLine();
				if(s == null || s.equals("") || s.equals("\r\n")){
					break;
				}
				System.out.println(s);  //print headers
			}
			
			//read request body
			String reqBody = "";
			System.out.println("Request Body");
			while(input.ready()){
				String s = input.readLine();
				if(s == null || s.equals("") || s.equals("\r\n")){
					break;
				}
				reqBody += s+"\n";
				System.out.println(s); //print request body
			}
			
			if(requestMethod == 2){ //POST METHOD
				String param = reqBody.split("\\n")[0];
				map.clear();
				String[] parameters = param.split("&"); 
				for(int i=0;i<parameters.length;i++){
					String[] tokens = parameters[i].split("=");
					map.put(tokens[0], tokens[1]);
				}
			}
			
			System.out.println("\nRequest line: " + requestline);	//Print request line
			
			System.out.println("Requested file: " + new File(requestedfile).getAbsolutePath() );	//Print requested file
			FileInputStream request = new FileInputStream(requestedfile);
			request.close();
			
			System.out.println("Sending 200");
			output.writeBytes(response(200,1));	//Make it 200
			System.out.println("200 sent");
			
			copyFilesToServer(new File(requestedfile).getName(), requestedfile); //copy to server directory
			System.out.println("\nSuccessfully copied requested file to server directory\n");
			writeHTML(map, output);
			
			output.close();
		}catch(FileNotFoundException f){
			try {//cant open file
		         output.writeBytes(response(404, 0));
		         output.close();
		         System.out.println("404 sent");
		      }
		    catch (Exception e) {
		    	  System.out.println("Error 4" + e.getMessage());
		    }
		}
		catch(Exception e){
			System.out.println("Error 5" + e.getMessage());
		}
	   
	}
	
	private void copyFilesToServer(String filename, String directory){	//Copy the file to server directory
		File source = new File(directory);
		File destination = new File(filename);
		InputStream in = null;
		OutputStream output = null;
		
		try{
			in = new FileInputStream(source);
			output = new FileOutputStream(destination);
			byte[] buff = new byte[1024];
			int bytes; 
			while((bytes = in.read(buff)) > 0){
				output.write(buff, 0, bytes);
			}
		}
		catch(FileNotFoundException e){
			System.out.println("Error 6" + e.getMessage());
		}
		catch(IOException e){
			System.out.println("Error 7" + e.getMessage());
		} finally{
			try{
				in.close();
				output.close();
			}
			catch(IOException e){
				System.out.println("Error 8" + e.getMessage());
			}
		}
	}

	private void writeHTML(Map<String, String> parameters, DataOutputStream output) throws IOException{ //Write in HTML Format 
		final String nl = "\r\n";
		createCSS();
		createJS();
		 StringBuilder sb = new StringBuilder();
		 	sb.append("<!DOCTYPE html>"+nl);
		    sb.append("<html>"+nl);
		    sb.append("<head>"+nl);
		     sb.append("<title>CMSC137 PROJECT 2</title>"+nl);
		    sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"./try.css\">"+nl);
		    sb.append("</head>"+nl);
		    sb.append("<body>"+nl);
		     sb.append("<h3>Mini Web Server</h3>"+nl);
	        sb.append("<script type=\"text/javascript\" src=\"./try.js\"></script>"+nl);
		    sb.append("<table>"+nl);
		    sb.append("<tr>"+nl);
		    sb.append("<th>Key</th>"+nl);
		    sb.append("<th>Value</th>"+nl);
		    sb.append("</tr>"+nl);
		    for(String key : parameters.keySet()){	//Print map values
		    	
			    sb.append("<tr>"+nl);
			    sb.append("<td>"+key+"</td>"+nl);
			    sb.append("<td>"+parameters.get(key)+"</td>"+nl);
			    sb.append("</tr>"+nl);
		    }
		    sb.append("</table>"+nl);
		    sb.append("</body>"+nl);
		    sb.append("</html>"+nl);
		   // System.out.println(sb);
		    output.writeBytes(sb.toString());
	}
	
	private void createCSS() throws IOException {
		final String nl = "\r\n";
		String fileName = "try.css";

        try {
            FileWriter fileWriter = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

		    bufferedWriter.write("html,body{");
		    bufferedWriter.newLine();
            bufferedWriter.write(" height: 100%;");
            bufferedWriter.newLine();
			bufferedWriter.write("}");
            bufferedWriter.newLine();
            bufferedWriter.write(" body{");
            bufferedWriter.newLine();
            bufferedWriter.write("font-size: 35px;");
            bufferedWriter.newLine();
            bufferedWriter.write("font-family: \"Times New Roman\", Times, serif;");
            bufferedWriter.newLine();
            bufferedWriter.write("color:red;");
            bufferedWriter.newLine();
            bufferedWriter.write("}");
            bufferedWriter.newLine();
            
            
            // Always close files.
            bufferedWriter.close();
            System.out.println("Successfully created try.css");
        }
        catch(IOException ex) {
            System.out.println(
                "Error writing to file '"+ fileName + "'");
        }
        
        
	}
	
	private void createJS() throws IOException {
		final String nl = "\r\n";
		String fileName = "try.js";

        try {
            FileWriter fileWriter = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

		    bufferedWriter.write("function myFunction() {");
		    bufferedWriter.newLine();
            bufferedWriter.write("alert(\"Hello! I am an alert box!\");");
            bufferedWriter.newLine();
			bufferedWriter.write("}");
            bufferedWriter.newLine();
            
            // Always close files.
            bufferedWriter.close();
            System.out.println("Successfully created try.js");
        }
        catch(IOException ex) {
            System.out.println(
                "Error writing to file '"+ fileName + "'");
        }
	}
	
	private String response(int result, int file3){	//Send response
		String x = "HTTP/1.1 ";
		switch(result){	//switch cases for responses
			case 200: x = x + "200 0K";break;
			case 400: x = x + "400 Bad Request";break;
			case 403: x = x + "403 Forbidden";break;
			case 404: x = x + "404 Not Found";break;
			case 500: x = x + "500 Internal Server Error";break;
			case 501: x = x + "403 Not Implemented";break;
		}
		x = x + "\r\n";
		x += "Connection: close\r\n";
		x += "Server: PatServer\r\n";
		String type = "Content-Type: ";
		switch(file3){
			case 0: type = ""; break;
			case 1: type = type + "text/html\r\n"; break; //html
			case 2: type = type + "text/css\r\n"; break; //css
			case 3: type = type + "application/javascript\r\n"; break; //js
		}
		x = x + type;
		System.out.println(x);
		return x + "\r\n";
	}
	
}

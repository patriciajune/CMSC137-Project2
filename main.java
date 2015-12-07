
public class main {

	
	static int listen_to_port = 8080;
	
	public static void main(String[] args) {
		
		miniwebserver server = new miniwebserver(listen_to_port);
		server.start();
	}

}


package cloudcentral.daemon;

import java.io.*;
import java.net.*;

public class FileExplorerDaemon {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		PipedInputStream[] pis = new PipedInputStream[16];
		PipedOutputStream[] pos = new PipedOutputStream[16];
		PipedReader[] LocalWebReader = new PipedReader[16];
		PipedWriter[] ClientWriter = new PipedWriter[16];
		PipedReader[] ClientReader = new PipedReader[16];
		PipedWriter[] LocalWebWriter = new PipedWriter[16];
		String names[] = new String[16];
		ClientThread[] clientThreads = new ClientThread[16];
		int connectedclients = 0;

		LocalWebThread local = new LocalWebThread(pis, LocalWebReader, LocalWebWriter, names);
		local.start();
		ServerSocket server = null;
		try {
			server = new ServerSocket(4396);
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		}
		while (true) {
			Socket s = server.accept();
			int i;
			try {
				for (i = 0, connectedclients = 0; i < 16; i++) {
					if (clientThreads[i] != null) {
						if (!clientThreads[i].isAlive()) {
							names[i] = null;
							pis[i].close();
							LocalWebReader[i].close();
							LocalWebWriter[i].close();
							clientThreads[i] = null;
						} else {
							connectedclients++;
						}
					}
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
				if (br.readLine().equals("<cloudcentral>")) {
					String ClientName = br.readLine();
					boolean sign_exists=false;
					for (i = 0; i < 16; i++) {

						if (names[i] == null) {
							pis[i] = new PipedInputStream();
							pos[i] = new PipedOutputStream();
							LocalWebReader[i] = new PipedReader();
							ClientReader[i] = new PipedReader();
							LocalWebWriter[i] = new PipedWriter();
							ClientWriter[i] = new PipedWriter();
							pis[i].connect(pos[i]);
							LocalWebReader[i].connect(ClientWriter[i]);
							LocalWebWriter[i].connect(ClientReader[i]);
							names[i] = ClientName;
							connectedclients++;
							break;
						}
						if(names[i].equals(ClientName))
						{
							sign_exists = true;
							break;
						}

					}
					if(sign_exists) {
						s.close();
						continue;
					}
					clientThreads[i] = new ClientThread(pos[i], ClientReader[i], ClientWriter[i], s);
					clientThreads[i].start();
					System.out.println("当前有" + connectedclients + "个请求");
				} else {
					s.close();
				}
			} catch (IOException e) {
				server.close();
				System.out.println(e);
				System.exit(1);
			}
		}
	}

}

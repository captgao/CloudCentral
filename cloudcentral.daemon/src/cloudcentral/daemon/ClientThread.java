package cloudcentral.daemon;

import java.io.*;
import java.net.*;

//与连接的客户端通信的线程，和LocalWebThread通过管道通信。可以在客户端断开连接时退出并返回消息。
public class ClientThread extends Thread {
	PipedOutputStream outstream;
	PipedReader reader;
	PipedWriter writer;
	Socket s;
	BufferedReader br;
	BufferedWriter bw;
	DataInputStream din;

	public ClientThread(PipedOutputStream outstream, PipedReader reader, PipedWriter writer, Socket s) {
		this.outstream = outstream;
		this.reader = reader;
		this.writer = writer;
		this.s = s;
	}

	public void run() {
		try {
			// 打开socket和管道;
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			din = new DataInputStream(s.getInputStream());
			BufferedWriter pbw;
			BufferedReader pbr;
			pbr = new BufferedReader(reader);
			pbw = new BufferedWriter(writer);
			// String rootpath=readFromClient();
			String pipereadbuf;
			while (true) {
				// System.out.println("ClientThread:while start");
				pipereadbuf = pbr.readLine();
				String type;
				System.out.println("ClientThread:require path " + pipereadbuf);
				// 向客户端发送信息，获取目标类型，如果客户端断开，将在这里产生IOException并结束线程。
				try {
					bw.write("<getmimetype>\n");
					bw.flush();
					bw.write(pipereadbuf + "\n");
					bw.flush();
					type = readFromClient();
				} catch (IOException e) {
					System.out.println("ClientThread:" + e);
					break;
				}
				// 通过管道发送给LocalWebThread
				System.out.println("ClientThread:type=" + type);
				pbw.write(type + "\n");
				pbw.flush();
				if (type.equals("<notfound>")) {
					continue;
				} else if (type.equals("<directory>")) { // 发送文件夹内容
					while (true) {
						String name = readFromClient();
						pbw.write(name + "\n");
						pbw.flush();
						if (name.equals("<enddir>"))
							break;
					}
				} else { // 传输文件
					System.out.println("ClientThread:read filelen");
					bw.write("sendlong\n");
					bw.flush();
					Long filelen = din.readLong();
					System.out.println("ClientThread:filelen=" + filelen);
					pbw.write(filelen.toString() + "\n");
					pbw.flush();
					byte[] buf = new byte[1024];
					int length = 0;
					long receivedlength = 0;
					// System.out.println("ClientThread:START SEND");
					// 文件传输，每传输一个缓存都有消息检测状态
					while (true) {
						String localthreadresponse = pbr.readLine();
						System.out.println("ClientThread:" + localthreadresponse);
						if (localthreadresponse == null) {
							bw.write("close\n");
							bw.flush();
							break;
						} else {
							if (localthreadresponse.equals("close")) {
								bw.write("close\n");
								bw.flush();
								break;
							} else {
								bw.write("reading\n");
								bw.flush();
							}
						}
						if (din != null) {
							length = din.read(buf);
						}
						outstream.write(buf, 0, length);
						outstream.flush();
						receivedlength += length;
						if (receivedlength == filelen)
							break;
					}
				}
				bw.write("completed\n");
				bw.flush();
			}
		} catch (IOException e) {
			System.out.println("ClientThread:" + e);
		}
	}

	private String readFromClient() throws IOException {
		String str = br.readLine();
		return str;
	}
}

package cloudcentral.daemon;

import java.io.*;
import java.net.*;

//�����ӵĿͻ���ͨ�ŵ��̣߳���LocalWebThreadͨ���ܵ�ͨ�š������ڿͻ��˶Ͽ�����ʱ�˳���������Ϣ��
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
			// ��socket�͹ܵ�;
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
				// ��ͻ��˷�����Ϣ����ȡĿ�����ͣ�����ͻ��˶Ͽ��������������IOException�������̡߳�
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
				// ͨ���ܵ����͸�LocalWebThread
				System.out.println("ClientThread:type=" + type);
				pbw.write(type + "\n");
				pbw.flush();
				if (type.equals("<notfound>")) {
					continue;
				} else if (type.equals("<directory>")) { // �����ļ�������
					while (true) {
						String name = readFromClient();
						pbw.write(name + "\n");
						pbw.flush();
						if (name.equals("<enddir>"))
							break;
					}
				} else { // �����ļ�
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
					// �ļ����䣬ÿ����һ�����涼����Ϣ���״̬
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

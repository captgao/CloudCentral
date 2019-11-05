package cloudcentral.client;

import java.io.*;
import java.net.*;

import java.util.HashMap;

import javax.activation.MimetypesFileTypeMap;

public class CloudCentralClient {
	public static void main(String[] args) throws IOException {
		Socket socket = null;
		try {// 当前路径下的clientconf包含了启动的配置信息
			File conf = new File("client.conf");
			BufferedReader fbr = new BufferedReader(new FileReader(conf));
			HashMap<String, String> map = new HashMap<String, String>();
			String line;
			while ((line = fbr.readLine()) != null) {
				String[] cache = line.split("=");
				map.put(cache[0], cache[cache.length - 1]);
			}
			fbr.close();
			String clientname = map.get("clientname");
			System.out.println("clientname=" + clientname);
			String rootpath = map.get("root");
			// 处理路径，无论路径是以/或\结尾还是没有符号，都可以识别
			if (!(rootpath.substring(rootpath.length() - 1).equals("/")
					|| rootpath.substring(rootpath.length() - 1).equals("\\"))) {
				rootpath += "/";
			}
			System.out.println("rootpath=" + rootpath);
			String server = map.get("server");
			int serverport = Integer.parseInt(map.get("serverport"));
			socket = new Socket(server, serverport);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
			String command;
			bw.write("<cloudcentral>\n");
			bw.flush();
			bw.write(clientname + "\n");
			bw.flush();
			while (true) {
				// System.out.println("while start");
				command = br.readLine();
				// System.out.println("readline complete");
				if (command.equals("<getmimetype>")) {
					String requestpath = br.readLine();
					File file = new File(rootpath + requestpath);
					if (file.exists()) {
						if (file.isDirectory()) {
							bw.write("<directory>\n");
							bw.flush();
							String[] list = file.list();
							for (String name : list) {
								bw.write(name + "\n");
								bw.flush();
								System.out.println(name);
							}
							bw.write("<enddir>\n");
							bw.flush();
							System.out.println("<end>");
						} else {
							String contentType = new MimetypesFileTypeMap().getContentType(file);
							bw.write(contentType + "\n");
							bw.flush();
							String sendlong = br.readLine();
							if (sendlong.equals("sendlong")) {
								dout.writeLong(file.length());
								dout.flush();
								System.out.println("writeLong");
								byte[] buf = new byte[1024];
								int length = 0;
								long sentlength = 0;
								FileInputStream fin = new FileInputStream(file);
								while (true) {
									String serverresponse = br.readLine();
									if (serverresponse == null) {
										break;
									} else if (serverresponse.equals("close")) {
										break;
									}
									if (fin != null)
										length = fin.read(buf);
									dout.write(buf, 0, length);
									dout.flush();
									sentlength += length;
									if (sentlength == file.length())
										break;
								}
								fin.close();
								System.out.println("completed");
							}
						}
					} else {
						bw.write("<notfound>\n");
						bw.flush();
					}
				}
				// System.out.println("end0");
				command = br.readLine();
			}
		} catch (IOException e) {
			socket.close();
			System.out.println(e);
		}

	}
}

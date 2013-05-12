/*******************************************************************************
 * Copyright (c) 2011 LSIIT - Université de Strasbourg
 * Copyright (c) 2011 Erkan VALENTIN <erkan.valentin[at]unistra.fr>
 * http://www.senslab.info/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.ecos.train;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrainManagerController {

	private Socket socket;
	private BufferedReader plecClient;
	private PrintWriter predClient;
	private boolean connected = false;

	public static int trainId = 1000;
	private static TrainManagerController instance = null;
	private static TrainManager activity;
	private static String RESPONSE_ERROR = "Can't read the ECoS response";

	private TrainManagerController() {
	}

	public static TrainManagerController getInstance() {
		if(instance == null)
			instance = new TrainManagerController();

		return instance;
	}

	public static void setActivity(TrainManager activity) {
		TrainManagerController.activity = activity;
	}

	/**
	 * open a socket for console
	 * @param consoleIp
	 * @param consolePort
	 * @throws IOException
	 */
	public void openSocket(String consoleIp, int consolePort) throws IOException {

		if(this.socket != null) 
			return;

		try {
			this.socket = new Socket();
			this.socket.connect(new InetSocketAddress(consoleIp, consolePort), 1000);
			this.plecClient = new BufferedReader(
					new InputStreamReader(this.socket.getInputStream()));
			this.predClient = new PrintWriter(
					new BufferedWriter(new OutputStreamWriter(
							this.socket.getOutputStream())),true);
		} catch (IOException e) {
			this.socket = null;
			throw e;
		}
	}

	/**
	 * set connected state
	 * @param state
	 */
	public void setConnected(boolean state) {
		this.connected = state;
	}

	/**
	 * get connected state
	 * @return
	 */
	public boolean isConnected() {
		return this.connected;
	}


	/**
	 * read a line on the soket
	 * @return
	 */
	public String readLine() {
		try{
			return plecClient.readLine();
		}
		catch(Exception e) {
			return null;
		}
	}

	/**
	 * close socket
	 * @throws IOException
	 */
	public void closeSocket() throws IOException {
		this.plecClient.close();
		this.predClient.close();
		this.socket.close();
		this.socket = null;
	}   


	/**
	 * send a command message to the console
	 * @param msg
	 */
	private String sendMsg(String msg) {
		this.predClient.println(msg);

		boolean quit = false;
		String fromServer = null;
		StringBuilder sb = new StringBuilder();

		try {
			while (quit == false && (fromServer = this.plecClient.readLine()) != null) {
				if (fromServer.startsWith("<REPLY")) {

				}
				else if (fromServer.startsWith("<END")) {
					quit = true;
				}
				else {
					sb.append(fromServer).append("\n");
				}
			}
		}
		catch(Exception e) {
		}


		return sb.toString().trim();
	}


	/**
	 * get emergency state
	 * @return
	 */
	public boolean getEmergencyState() {

		String result = this.sendMsg("get(1, status)");

		int index1 = result.lastIndexOf('[');
		int index2 = result.lastIndexOf(']');

		try {
			return result.substring(index1+1, index2).toUpperCase().equals("STOP");
		}
		catch(Exception s) {
			return false;
		}
	}


	/**
	 * trigger emergency stop
	 * @param state
	 */
	public void emergencyStop(boolean state) {
		if(state) {
			this.sendMsg("set(1, stop)");
		}
		else {
			this.sendMsg("set(1, go)");
		}
	}

	/**
	 * get info about console
	 * TODO: return console information
	 */
	public void getInfo() {
		this.sendMsg("get(1, info)");
	}


	/**
	 * get name of a train
	 * @return name
	 */
	public String getName() {
		String result = this.sendMsg("get("+TrainManagerController.trainId+", name)");

		int index1 = result.lastIndexOf('[');
		int index2 = result.lastIndexOf(']');

		try {
			return result.substring(index1+1, index2);
		}
		catch(Exception s) {
			TrainManagerController.activity.displayError(TrainManagerController.RESPONSE_ERROR);
			return "";
		}
	}

	/**
	 * get speed of a train
	 * @return
	 */
	public int getSpeed() {
		String result = this.sendMsg("get("+TrainManagerController.trainId+", speed)");

		int index1 = result.lastIndexOf('[');
		int index2 = result.lastIndexOf(']');

		try {
			return Integer.parseInt(result.substring(index1+1, index2));
		}
		catch(Exception s) {
			return 0;
		}
	}


	/**
	 * set speed of the train
	 * @param speed
	 */
	public void setSpeed(int speed) {
		this.sendMsg("set("+TrainManagerController.trainId+", speed["+speed+"])");
	}

	/**
	 * get direction of the train
	 * @return true = GO_FORWARD
	 */
	public boolean getDir() {

		String result = this.sendMsg("get("+TrainManagerController.trainId+", dir)");

		int index1 = result.lastIndexOf('[');
		int index2 = result.lastIndexOf(']');

		try {
			return Integer.parseInt(result.substring(index1+1, index2)) == 0
					? true : false;
		}
		catch(Exception s) {
			return false;
		}

	}

	/**
	 * set direction of the train
	 * @param true = GO_FORWARD
	 */
	public void setDir(int dir) {
		this.sendMsg("set("+TrainManagerController.trainId+", dir["+dir+"])");
	}

	/**
	 * get list of train
	 * TODO: test this with multiple trains
	 */
	public List<String> getTrains() {

		List<String> trainId = new ArrayList<String>();

		try {
			String result = this.sendMsg("queryObjects(10)");
			String list[] = result.split("\n");
			for(int i=0; i<list.length; i++) {
				trainId.add(list[i]);
			}
		}
		catch(Exception e) {

		}
		return trainId;
	}

	/**
	 * return id, name and address of the train
	 * @return
	 */
	public List<Train> getFullTrains() {

		List<Train> trainId = new ArrayList<Train>();

		String result = this.sendMsg("queryObjects(10, name, addr)");

		String list[] = result.split("\n");
		Pattern p = Pattern.compile("(.*) name\\[\"(.*)\"\\] addr\\[(.*)\\]");
		String id = "";
		String addr = "";
		String name = "";

		for(int i=0; i<list.length; i++) {
			Matcher m = p.matcher(list[i]);

			while (m.find() == true) {
				id = m.group(1).trim();
				name = m.group(2).trim();
				addr = m.group(3).trim();
			}

			trainId.add(new Train(id, name, addr));
		}

		return trainId;
	}

	/**
	 * take control of the train
	 */
	public void takeControl() {
		this.sendMsg("request("+TrainManagerController.trainId+", control, force)");
	}

	/**
	 * release control of the train
	 */
	public void releaseControl() {
		this.sendMsg("release("+TrainManagerController.trainId+", control)");
	}

	/**
	 * set button state
	 * @param i
	 * @param enabled
	 */
	public void setButton(int i, boolean enabled) {
		int value = (enabled) ? 1 : 0;
		this.sendMsg("set("+TrainManagerController.trainId+", func["+i+", "+value+"])");
	}

	/**
	 * get button state
	 * @param i
	 * @return
	 */
	public boolean getButton(int i) {
		String result = this.sendMsg("get("+TrainManagerController.trainId+", func["+i+"])");

		int index1 = result.lastIndexOf(' ');
		int index2 = result.lastIndexOf(']');

		try {
			return Integer.parseInt(result.substring(index1+1, index2)) == 1
					? true : false;
		}
		catch(Exception s) {
			return false;
		}

	}
}


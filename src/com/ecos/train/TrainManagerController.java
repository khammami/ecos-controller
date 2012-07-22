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

public class TrainManagerController {

	private Socket socket;
	private BufferedReader plecClient;
	private PrintWriter predClient;
	private static TrainManagerController instance = null;
	public static int trainId = 1000;
	private static TrainManager activity;
	private static String RESPONSE_ERROR = "Can't read the ECoS response";

	private boolean connected = false;

	/**
	 * 
	 * @param activity
	 */
	private TrainManagerController() {
	}

	/**
	 * 
	 * @return
	 */
	public static TrainManagerController getInstance() {
		if(instance == null)
			instance = new TrainManagerController();

		return instance;
	}

	/**
	 * 
	 * @param activity
	 */
	public static void setActivity(TrainManager activity) {
		TrainManagerController.activity = activity;
	}

	/**
	 * 
	 * @param consoleIp
	 * @param consolePort
	 * @throws IOException
	 */
	public void openSocket(String consoleIp, int consolePort, int trainId) throws IOException {

		TrainManagerController.trainId = trainId;

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
	 * 
	 * @param state
	 */
	public void setConnected(boolean state) {
		this.connected = state;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return this.connected;
	}


	/**
	 * 
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
	 * 
	 * @throws IOException
	 */
	public void closeSocket() throws IOException {
		this.plecClient.close();
		this.predClient.close();
		this.socket.close();
		this.socket = null;
	}   


	/**
	 * 
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
	 * 
	 */
	public boolean getEmergencyState() {

		String result = this.sendMsg("get(1, status)");

		int index1 = result.lastIndexOf('[');
		int index2 = result.lastIndexOf(']');

		try {
			return result.substring(index1+1, index2).toUpperCase().equals("GO");
		}
		catch(Exception s) {
			TrainManagerController.activity.displayError(TrainManagerController.RESPONSE_ERROR);
			return false;
		}
	}


	/**
	 * 
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
	 * 
	 */
	public void getInfo() {
		this.sendMsg("get(1, info)");
	}


	/**
	 * 
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
			TrainManagerController.activity.displayError(TrainManagerController.RESPONSE_ERROR);
			return 0;
		}
	}


	/**
	 * 
	 * @param speed
	 */
	public void setSpeed(int speed) {
		this.sendMsg("set("+TrainManagerController.trainId+", speed["+speed+"])");
	}

	/**
	 * 
	 * @return true if direction is GO_FORWARD
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
			TrainManagerController.activity.displayError(TrainManagerController.RESPONSE_ERROR);;
			return false;
		}

	}

	/**
	 * 
	 * @param x
	 */
	public void setDir(int x) {
		this.sendMsg("set("+TrainManagerController.trainId+", dir["+x+"])");
	}

	/**
	 * 
	 */
	public void getLoco() {
		this.sendMsg("queryObjects(10)");
	}

	/**
	 * 
	 */
	public void takeControl() {
		this.sendMsg("request("+TrainManagerController.trainId+", control, force)");
	}

	/**
	 * 
	 */
	public void releaseControl() {
		this.sendMsg("release("+TrainManagerController.trainId+", control)");
	}

	/**
	 * 
	 */
	public void setButton(int i, boolean enabled) {
		int value = (enabled) ? 1 : 0;
		this.sendMsg("set("+TrainManagerController.trainId+", func["+i+", "+value+"])");
	}

	/**
	 * 
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
			TrainManagerController.activity.displayError(TrainManagerController.RESPONSE_ERROR);
			return false;
		}

	}
}

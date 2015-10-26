package com.app2.remote;

import java.io.IOException;
import java.util.Scanner;

import com.app2.remote.Network.ChatMessage;
import com.app2.remote.Network.RegisterName;
import com.app2.remote.utils.ComputerInfo;
import com.app2.remote.utils.Configuration;
import com.app2.remote.utils.Log;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class Work {
    private static final String TAG = "fuyao-Work";

    private static final Object mLock = new Object();

    private Client mRemoteClient;

    private String mCurrentDevice = null;

    public void start() {
        showWait();
        mRemoteClient = new Client();
        mRemoteClient.start();
        Network.register(mRemoteClient);
        mRemoteClient.addListener(mRemoteClientListener);
        try {
            Log.d(TAG, "start connect remote server");
            mRemoteClient.connect(5000, Configuration.MANAGER_SERVER_IP, Configuration.MANAGER_SERVER_PORT);
            Log.d(TAG, "start connect remote over 1");
        } catch (IOException ex) {
            Log.d(TAG, "IOException " + ex.getMessage());
            showFail();
            return;
        }
        synchronized (mLock) {
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void cmdListDevices() {
        ChatMessage msg = new ChatMessage();
        msg.text = Configuration.CMD_LIST_DEVICES;
        mRemoteClient.sendTCP(msg);
    }

    private void cmdGetDevice() {
        ChatMessage msg = new ChatMessage();
        msg.text = Configuration.CMD_GET_DEVICE;
        mRemoteClient.sendTCP(msg);
    }
    
    private void showFail() {
        System.out.println("Connect fail, Ask server manger to help!");
    }

    private void showWait() {
        System.out.println("Connecting... Please wait!");
    }

    private void startChoseDevice(String[] devices) {
        Scanner scanner = new Scanner(System.in);
        String temp = scanner.nextLine();

        boolean isString = false;
        int index = -1;
        try {
            index = Integer.valueOf(temp);
        } catch (NumberFormatException e) {
            isString = true;
        }

        if (isString) {
            if (temp.equals("exit")) {
                System.out.println("Device not set,Bye!");
                //mRemoteClient.stop();
                System.exit(1);
            } else {
                System.out.println("Command Error!");
                showDevices(devices);
                startChoseDevice(devices);
            }
        } else if (index == 0) {
            cmdListDevices();
        } else if (null != devices && 0 < index && index <= devices.length) {
            setDevices(devices[index - 1]);
        } else {
            showDevices(devices);
            startChoseDevice(devices);
        }
    }

    private void setDevices(String device) {
        ChatMessage msg = new ChatMessage();
        msg.text = Configuration.CMD_SET_DEVICE + device;
        mRemoteClient.sendTCP(msg);
    }

    private void showDevices(String[] devices) {
        String out = "";
        if (null != devices && devices.length > 0) {
            for (int i = 1; i <= devices.length; i++) {
                if (null != mCurrentDevice && null != devices[i - 1] && mCurrentDevice.equals(devices[i - 1])) {
                    out += i + " ---- " + devices[i - 1] + " CURRENT DEVICE!\n";
                } else {
                    out += i + " ---- " + devices[i - 1] + "\n";
                }
            }

            out += "Chose index to set device (1~" + (devices.length) + ")";
        } else {
            out = "Not found device!\nInput 0 to update devices!\nInput exit to exit!";
        }
        System.out.println(out);
    }

    Listener mRemoteClientListener = new Listener() {

        @Override
        public void connected(Connection connection) {
            RegisterName registerName = new RegisterName();
            registerName.name = Configuration.TYPE_PC_TERMINAL + "-" + ComputerInfo.getMacAddress();
            mRemoteClient.sendTCP(registerName);
            cmdGetDevice();
            cmdListDevices();
            Log.d(TAG, "connect remote server ok!");
        }

        @Override
        public void disconnected(Connection connection) {
            mRemoteClient.stop();
            System.out.println("connection broken, please retry!");
            System.exit(1);
        }

        @Override
        public void received(Connection connection, Object object) {

            if (object instanceof ChatMessage) {
                ChatMessage chatMessage = (ChatMessage) object;
                Log.d(TAG, "receive text: " + chatMessage.text);

                if ((null != chatMessage.text) && chatMessage.text.startsWith(Configuration.CMD_RETRUN_LIST_DEVICES)) {
                    String temp = chatMessage.text.substring(Configuration.CMD_RETRUN_LIST_DEVICES.length()).trim();
                    final String[] devices = (null == temp || temp.equals("")) ? null : temp.split(Configuration.SEG);
                    showDevices(devices);
                    new Thread() {
                        @Override
                        public void run() {
                            startChoseDevice(devices);
                        }

                    }.start();
                } else if ((null != chatMessage.text) && chatMessage.text.startsWith(Configuration.CMD_RETRUN_SET_DEVICE)) {
                    String temp = chatMessage.text.substring(Configuration.CMD_RETRUN_SET_DEVICE.length());
                    if (null == temp) {

                    } else if ("fail".equals(temp)) {

                    } else {
                        //mRemoteClient.stop();
                        System.out.println("set device ok,Bye!");
                        System.exit(1);
                    }
                } else if ((null != chatMessage.text) && chatMessage.text.startsWith(Configuration.CMD_RETRUN_GET_DEVICE)) {
                    String temp = chatMessage.text.substring(Configuration.CMD_RETRUN_GET_DEVICE.length());
                    if (null == temp || temp.equals("")) {

                    } else {
                        mCurrentDevice = temp;
                    }
                }
                return;
            }
        }

        @Override
        public void idle(Connection connection) {
            // TODO Auto-generated method stub
            super.idle(connection);
        }
    };
}

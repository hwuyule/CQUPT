package com.ccf.feige.activity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ccf.feige.adapter.ChatListAdapter;
import com.ccf.feige.data.ChatMessage;
import com.ccf.feige.data.Person;
import com.ccf.feige.interfaces.ReceiveMsgListener;
import com.ccf.feige.net.NetTcpFileSendThread;
import com.ccf.feige.utils.IpMessageConst;
import com.ccf.feige.utils.IpMessageProtocol;
import com.ccf.feige.utils.UsedConst;

/**
 * ���촰��activity
 * @author ccf
 * 
 * 2012/2/21
 *
 */
public class MyFeiGeChatActivity extends MyFeiGeBaseActivity implements OnClickListener,ReceiveMsgListener{
	
//	private NetThreadHelper netThreadHelper;
	
	
//	private ImageView chat_item_head;	//ͷ��
	private TextView chat_name;			//���ּ�IP
	private TextView chat_mood;			//����
	private Button chat_quit;			//�˳���ť
	private ListView chat_list;			//�����б�
	private EditText chat_input;		//���������
	private Button chat_send;			//���Ͱ�ť
	private Button chat_record;			//������ť
	
	private List<ChatMessage> msgList;	//������ʾ����Ϣlist
	private String receiverName;			//Ҫ���ձ�activity�����͵���Ϣ���û�����
	private String receiverIp;			//Ҫ���ձ�activity�����͵���Ϣ���û�IP
	private String receiverGroup;			//Ҫ���ձ�activity�����͵���Ϣ���û�����
	private ChatListAdapter adapter;	//ListView��Ӧ��adapter
	private String selfName;
	private String selfGroup;
	
	private final static int MENU_ITEM_SENDFILE = Menu.FIRST;	//�����ļ�
	private final static int MENU_ITEM_EXIT = Menu.FIRST + 1;
	private boolean isStopTalk = false;//ͨ��������־

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
		
		findViews();
		
//		netThreadHelper = NetThreadHelper.newInstance();
		msgList = new ArrayList<ChatMessage>();
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		receiverName = bundle.getString("receiverName");
		receiverIp = bundle.getString("receiverIp");
		receiverGroup = bundle.getString("receiverGroup");
		selfName = "android�ɸ�";
		selfGroup = "android";
		
		chat_name.setText(receiverName + "(" + receiverIp + ")");
		chat_mood.setText("������" + receiverGroup);
		chat_quit.setOnClickListener(this);
		chat_send.setOnClickListener(this);
		chat_record.setOnClickListener(this);
		Iterator<ChatMessage> it = netThreadHelper.getReceiveMsgQueue().iterator();
		while(it.hasNext()){	//ѭ����Ϣ���У���ȡ�������뱾����activity�����Ϣ
			ChatMessage temp = it.next();
			//����Ϣ�����еķ������뱾activity����Ϣ������IP��ͬ���������Ϣ�ó�����ӵ���activityҪ��ʾ����Ϣlist��
			if(receiverIp.equals(temp.getSenderIp())){ 
				msgList.add(temp);	//��ӵ���ʾlist
				it.remove();		//������Ϣ����Ϣ�������Ƴ�
			}
		}
		
		adapter = new ChatListAdapter(this, msgList);
		chat_list.setAdapter(adapter);
		
		netThreadHelper.addReceiveMsgListener(this);	//ע�ᵽlisteners
	}
	
	private void findViews(){
//		chat_item_head = (ImageView) findViewById(R.id.chat_item_head);
		chat_name = (TextView) findViewById(R.id.chat_name);
		chat_mood = (TextView) findViewById(R.id.chat_mood);
		chat_quit = (Button) findViewById(R.id.chat_quit);
		chat_list = (ListView) findViewById(R.id.chat_list);
		chat_input = (EditText) findViewById(R.id.chat_input);
		chat_send = (Button) findViewById(R.id.chat_send);
		chat_record = (Button) findViewById(R.id.chat_record);
	}

	@Override
	public void processMessage(Message msg) {
		// TODO Auto-generated method stub
		switch(msg.what){
		case IpMessageConst.IPMSG_SENDMSG:
			adapter.notifyDataSetChanged();	//ˢ��ListView
			break;
			
		case IpMessageConst.IPMSG_RELEASEFILES:{ //�ܾ������ļ�,ֹͣ�����ļ��߳�
			if(NetTcpFileSendThread.server != null){
				try {
					NetTcpFileSendThread.server.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
			break;
			
		case UsedConst.FILESENDSUCCESS:{	//�ļ����ͳɹ�
			makeTextShort("�ļ����ͳɹ�");
		}
			break;
			
			
		}	//end of switch
	}

	@Override
	public boolean receive(ChatMessage msg) {
		// TODO Auto-generated method stub
		if(receiverIp.equals(msg.getSenderIp())){	//����Ϣ�뱾activity�йأ������
			msgList.add(msg);	//������Ϣ��ӵ���ʾlist��
			sendEmptyMessage(IpMessageConst.IPMSG_SENDMSG); //ʹ��handle֪ͨ��������UI
			MyFeiGeBaseActivity.playMsg();
			return true;
		}
		
		return false;
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		//һ��Ҫ�Ƴ�����Ȼ��Ϣ���ջ��������
		netThreadHelper.removeReceiveMsgListener(this);
		super.finish();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v == chat_send){
			sendAndAddMessage();	
		}else if(v == chat_quit){
			finish();
		}
		else if(v == chat_record)
		{
			sendAndAddRecord();
		}
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		isRecording = false; 
	}

	/**
	 * ������Ϣ��������Ϣ��ӵ�UI��ʾ
	 */
	private void sendAndAddMessage(){
		String msgStr = chat_input.getText().toString().trim();
		if(!"".equals(msgStr)){
			//������Ϣ
			IpMessageProtocol sendMsg = new IpMessageProtocol();
			sendMsg.setVersion(String.valueOf(IpMessageConst.VERSION));
			sendMsg.setSenderName(selfName);
			sendMsg.setSenderHost(selfGroup);
			sendMsg.setCommandNo(IpMessageConst.IPMSG_SENDMSG);
			sendMsg.setAdditionalSection(msgStr);
			InetAddress sendto = null;
			try {
				sendto = InetAddress.getByName(receiverIp);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				Log.e("MyFeiGeChatActivity", "���͵�ַ����");
			}
			if(sendto != null)
				netThreadHelper.sendUdpData(sendMsg.getProtocolString() + "\0", sendto, IpMessageConst.PORT);
			
			//�����Ϣ����ʾlist
			ChatMessage selfMsg = new ChatMessage("localhost", selfName, msgStr, new Date());
			selfMsg.setSelfMsg(true);	//����Ϊ������Ϣ
			msgList.add(selfMsg);	
			
		}else{
			makeTextShort("���ܷ��Ϳ�����");
		}
		
		chat_input.setText("");
		adapter.notifyDataSetChanged();//����UI
	}
	
	private void sendAndAddRecord()
	{
        isRecording = true;  
        new RecordPlayThread().start();// ��һ���̱߳�¼�߷�   
	}
	
    class RecordPlayThread extends Thread {  
        public void run() {  
            try {  
                byte[] buffer = new byte[recBufSize];  
                audioRecord.startRecording();//��ʼ¼��   
               audioTrack.play();//��ʼ����   
                  
                while (isRecording) {  
                    //��MIC�������ݵ�������   
                    int bufferReadResult = audioRecord.read(buffer, 0,  
                            recBufSize);  
  
                    byte[] tmpBuf = new byte[bufferReadResult];  
                    System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);  
                    //д�����ݼ�����   
                    audioTrack.write(tmpBuf, 0, tmpBuf.length);  
                }  
                audioTrack.stop();  
                audioRecord.stop();  
            } catch (Throwable t) {  
                Toast.makeText(MyFeiGeChatActivity.this, t.getMessage(), 1000);  
            }  
        }  
    }; 
    //=========================TCP��������ģ��==================================================================    
	//����Tcp��������ģ��
	private class AudioHandler extends Thread{
		private ServerSocket sSocket = null;
		
	//	private G711Codec codec;
		public AudioHandler(){}
		@Override
		public void run() {
			super.run();
			try {
				sSocket = new ServerSocket(IpMessageConst.AUDIO_PORT);//������Ƶ�˿�
				System.out.println("Audio Handler socket started ...");
				while(!sSocket.isClosed() && null!=sSocket){
					Socket socket = sSocket.accept();
					socket.setSoTimeout(5000);
					audioPlay(socket);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//����������Ƶ�������߳�
		public void audioPlay(Socket socket){
			new AudioPlay(socket).start();
		}
		//����������Ƶ�������߳�
		public void audioSend(Person person){
			new AudioSend(person).start();
		}
		
		//��Ƶ���߳�
		public class AudioPlay extends Thread{
			Socket socket = null;
			public AudioPlay(Socket socket){
				this.socket = socket;
			//	android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO); 
			}
			
			@Override
			public void run() {
				super.run();
				try {
					InputStream is = socket.getInputStream();
					//�����Ƶ��������С
					int bufferSize = android.media.AudioTrack.getMinBufferSize(8000,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);

					//����������
					AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, 
							8000,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT,
							bufferSize,
							AudioTrack.MODE_STREAM);

					//������������
					player.setStereoVolume(1.0f, 1.0f);
					//��ʼ��������
					player.play();
					byte[] audio = new byte[160];//��Ƶ��ȡ����
					int length = 0;
					
					while(!isStopTalk){
						length = is.read(audio);//�������ȡ��Ƶ����
						if(length>0 && length%2==0){
						//	for(int i=0;i<length;i++)audio[i]=(byte)(audio[i]*2);//��Ƶ�Ŵ�1��
							player.write(audio, 0, length);//������Ƶ����
						}
					}
					player.stop();
					is.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//��Ƶ�����߳�
		public class AudioSend extends Thread{
			Person person = null;
			
			public AudioSend(Person person){
				this.person = person;
			//	android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO); 
			}
			@Override
			public void run() {
				super.run();
				Socket socket = null;
				OutputStream os = null;
				AudioRecord recorder = null;
				try {
					socket = new Socket(person.ipAddress, IpMessageConst.AUDIO_PORT);
					socket.setSoTimeout(5000);
					os = socket.getOutputStream();
					//���¼����������С
					int bufferSize = AudioRecord.getMinBufferSize(8000,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
					
					//���¼��������
					recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
							8000,AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT,
							bufferSize*10);
					
					recorder.startRecording();//��ʼ¼��
					byte[] readBuffer = new byte[640];//¼��������
					
					int length = 0;
					
					while(!isStopTalk){
						length = recorder.read(readBuffer,0,640);//��mic��ȡ��Ƶ����
						if(length>0 && length%2==0){
							os.write(readBuffer,0,length);//д�뵽�����������Ƶ����ͨ�����緢�͸��Է�
						}
					}
					recorder.stop();
					os.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public void release() {
			try {
				System.out.println("Audio handler socket closed ...");
				if(null!=sSocket)sSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	//=========================TCP��������ģ�����================================================================== 

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ITEM_SENDFILE, 0, "�����ļ�");
		menu.add(0, MENU_ITEM_EXIT, 0, "�˳�");
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
		case MENU_ITEM_SENDFILE:
			Intent intent = new Intent(this, MyFeiGeFileActivity.class);
			startActivityForResult(intent, 0);
			
			break;
		case MENU_ITEM_EXIT:
			finish();
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK){
			//�õ������ļ���·��
			Bundle bundle = data.getExtras();
			
			String filePaths = bundle.getString("filePaths");	//�����ļ���Ϣ��,����ļ�ʹ��"\0"���зָ�
//			Toast.makeText(this, filePaths, Toast.LENGTH_SHORT).show();
			
			String[] filePathArray = filePaths.split("\0");
			
			
			//���ʹ����ļ�UDP���ݱ�
			IpMessageProtocol sendPro = new IpMessageProtocol();
			sendPro.setVersion("" +IpMessageConst.VERSION);
			sendPro.setCommandNo(IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT);
			sendPro.setSenderName(selfName);
			sendPro.setSenderHost(selfGroup);
			String msgStr = "";	//���͵���Ϣ
			
			StringBuffer additionInfoSb = new StringBuffer();	//������ϸ����ļ���ʽ��sb
			for(String path:filePathArray){
				File file = new File(path);
				additionInfoSb.append("0:");
				additionInfoSb.append(file.getName() + ":");
				additionInfoSb.append(Long.toHexString(file.length()) + ":");		//�ļ���Сʮ�����Ʊ�ʾ
				additionInfoSb.append(Long.toHexString(file.lastModified()) + ":");	//�ļ�����ʱ�䣬������ʱ������޸�ʱ�����
				additionInfoSb.append(IpMessageConst.IPMSG_FILE_REGULAR + ":");
				byte[] bt = {0x07};		//���ڷָ���������ļ����ַ�
				String splitStr = new String(bt);
				additionInfoSb.append(splitStr);
			}
			
			sendPro.setAdditionalSection(msgStr + "\0" + additionInfoSb.toString() + "\0");
			
			InetAddress sendto = null;
			try {
				sendto = InetAddress.getByName(receiverIp);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				Log.e("MyFeiGeChatActivity", "���͵�ַ����");
			}
			if(sendto != null)
				netThreadHelper.sendUdpData(sendPro.getProtocolString(), sendto, IpMessageConst.PORT);
			
			//����2425�˿ڣ�׼������TCP��������
			Thread netTcpFileSendThread = new Thread(new NetTcpFileSendThread(filePathArray));
			netTcpFileSendThread.start();	//�����߳�
		}
	}
	

}

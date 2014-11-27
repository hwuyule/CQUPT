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
 * 聊天窗口activity
 * @author ccf
 * 
 * 2012/2/21
 *
 */
public class MyFeiGeChatActivity extends MyFeiGeBaseActivity implements OnClickListener,ReceiveMsgListener{
	
//	private NetThreadHelper netThreadHelper;
	
	
//	private ImageView chat_item_head;	//头像
	private TextView chat_name;			//名字及IP
	private TextView chat_mood;			//组名
	private Button chat_quit;			//退出按钮
	private ListView chat_list;			//聊天列表
	private EditText chat_input;		//聊天输入框
	private Button chat_send;			//发送按钮
	private Button chat_record;			//语音按钮
	
	private List<ChatMessage> msgList;	//用于显示的消息list
	private String receiverName;			//要接收本activity所发送的消息的用户名字
	private String receiverIp;			//要接收本activity所发送的消息的用户IP
	private String receiverGroup;			//要接收本activity所发送的消息的用户组名
	private ChatListAdapter adapter;	//ListView对应的adapter
	private String selfName;
	private String selfGroup;
	
	private final static int MENU_ITEM_SENDFILE = Menu.FIRST;	//发送文件
	private final static int MENU_ITEM_EXIT = Menu.FIRST + 1;
	private boolean isStopTalk = false;//通话结束标志

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
		selfName = "android飞鸽";
		selfGroup = "android";
		
		chat_name.setText(receiverName + "(" + receiverIp + ")");
		chat_mood.setText("组名：" + receiverGroup);
		chat_quit.setOnClickListener(this);
		chat_send.setOnClickListener(this);
		chat_record.setOnClickListener(this);
		Iterator<ChatMessage> it = netThreadHelper.getReceiveMsgQueue().iterator();
		while(it.hasNext()){	//循环消息队列，获取队列中与本聊天activity相关信息
			ChatMessage temp = it.next();
			//若消息队列中的发送者与本activity的消息接收者IP相同，则将这个消息拿出，添加到本activity要显示的消息list中
			if(receiverIp.equals(temp.getSenderIp())){ 
				msgList.add(temp);	//添加到显示list
				it.remove();		//将本消息从消息队列中移除
			}
		}
		
		adapter = new ChatListAdapter(this, msgList);
		chat_list.setAdapter(adapter);
		
		netThreadHelper.addReceiveMsgListener(this);	//注册到listeners
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
			adapter.notifyDataSetChanged();	//刷新ListView
			break;
			
		case IpMessageConst.IPMSG_RELEASEFILES:{ //拒绝接受文件,停止发送文件线程
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
			
		case UsedConst.FILESENDSUCCESS:{	//文件发送成功
			makeTextShort("文件发送成功");
		}
			break;
			
			
		}	//end of switch
	}

	@Override
	public boolean receive(ChatMessage msg) {
		// TODO Auto-generated method stub
		if(receiverIp.equals(msg.getSenderIp())){	//若消息与本activity有关，则接收
			msgList.add(msg);	//将此消息添加到显示list中
			sendEmptyMessage(IpMessageConst.IPMSG_SENDMSG); //使用handle通知，来更新UI
			MyFeiGeBaseActivity.playMsg();
			return true;
		}
		
		return false;
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		//一定要移除，不然信息接收会出现问题
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
	 * 发送消息并将该消息添加到UI显示
	 */
	private void sendAndAddMessage(){
		String msgStr = chat_input.getText().toString().trim();
		if(!"".equals(msgStr)){
			//发送消息
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
				Log.e("MyFeiGeChatActivity", "发送地址有误");
			}
			if(sendto != null)
				netThreadHelper.sendUdpData(sendMsg.getProtocolString() + "\0", sendto, IpMessageConst.PORT);
			
			//添加消息到显示list
			ChatMessage selfMsg = new ChatMessage("localhost", selfName, msgStr, new Date());
			selfMsg.setSelfMsg(true);	//设置为自身消息
			msgList.add(selfMsg);	
			
		}else{
			makeTextShort("不能发送空内容");
		}
		
		chat_input.setText("");
		adapter.notifyDataSetChanged();//更新UI
	}
	
	private void sendAndAddRecord()
	{
        isRecording = true;  
        new RecordPlayThread().start();// 开一条线程边录边放   
	}
	
    class RecordPlayThread extends Thread {  
        public void run() {  
            try {  
                byte[] buffer = new byte[recBufSize];  
                audioRecord.startRecording();//开始录制   
               audioTrack.play();//开始播放   
                  
                while (isRecording) {  
                    //从MIC保存数据到缓冲区   
                    int bufferReadResult = audioRecord.read(buffer, 0,  
                            recBufSize);  
  
                    byte[] tmpBuf = new byte[bufferReadResult];  
                    System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);  
                    //写入数据即播放   
                    audioTrack.write(tmpBuf, 0, tmpBuf.length);  
                }  
                audioTrack.stop();  
                audioRecord.stop();  
            } catch (Throwable t) {  
                Toast.makeText(MyFeiGeChatActivity.this, t.getMessage(), 1000);  
            }  
        }  
    }; 
    //=========================TCP语音传输模块==================================================================    
	//基于Tcp语音传输模块
	private class AudioHandler extends Thread{
		private ServerSocket sSocket = null;
		
	//	private G711Codec codec;
		public AudioHandler(){}
		@Override
		public void run() {
			super.run();
			try {
				sSocket = new ServerSocket(IpMessageConst.AUDIO_PORT);//监听音频端口
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
		//用来启动音频播放子线程
		public void audioPlay(Socket socket){
			new AudioPlay(socket).start();
		}
		//用来启动音频发送子线程
		public void audioSend(Person person){
			new AudioSend(person).start();
		}
		
		//音频播线程
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
					//获得音频缓冲区大小
					int bufferSize = android.media.AudioTrack.getMinBufferSize(8000,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);

					//获得音轨对象
					AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, 
							8000,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT,
							bufferSize,
							AudioTrack.MODE_STREAM);

					//设置喇叭音量
					player.setStereoVolume(1.0f, 1.0f);
					//开始播放声音
					player.play();
					byte[] audio = new byte[160];//音频读取缓存
					int length = 0;
					
					while(!isStopTalk){
						length = is.read(audio);//从网络读取音频数据
						if(length>0 && length%2==0){
						//	for(int i=0;i<length;i++)audio[i]=(byte)(audio[i]*2);//音频放大1倍
							player.write(audio, 0, length);//播放音频数据
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
		
		//音频发送线程
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
					//获得录音缓冲区大小
					int bufferSize = AudioRecord.getMinBufferSize(8000,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
					
					//获得录音机对象
					recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
							8000,AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT,
							bufferSize*10);
					
					recorder.startRecording();//开始录音
					byte[] readBuffer = new byte[640];//录音缓冲区
					
					int length = 0;
					
					while(!isStopTalk){
						length = recorder.read(readBuffer,0,640);//从mic读取音频数据
						if(length>0 && length%2==0){
							os.write(readBuffer,0,length);//写入到输出流，把音频数据通过网络发送给对方
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
	//=========================TCP语音传输模块结束================================================================== 

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ITEM_SENDFILE, 0, "发送文件");
		menu.add(0, MENU_ITEM_EXIT, 0, "退出");
		
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
			//得到发送文件的路径
			Bundle bundle = data.getExtras();
			
			String filePaths = bundle.getString("filePaths");	//附加文件信息串,多个文件使用"\0"进行分隔
//			Toast.makeText(this, filePaths, Toast.LENGTH_SHORT).show();
			
			String[] filePathArray = filePaths.split("\0");
			
			
			//发送传送文件UDP数据报
			IpMessageProtocol sendPro = new IpMessageProtocol();
			sendPro.setVersion("" +IpMessageConst.VERSION);
			sendPro.setCommandNo(IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT);
			sendPro.setSenderName(selfName);
			sendPro.setSenderHost(selfGroup);
			String msgStr = "";	//发送的消息
			
			StringBuffer additionInfoSb = new StringBuffer();	//用于组合附加文件格式的sb
			for(String path:filePathArray){
				File file = new File(path);
				additionInfoSb.append("0:");
				additionInfoSb.append(file.getName() + ":");
				additionInfoSb.append(Long.toHexString(file.length()) + ":");		//文件大小十六进制表示
				additionInfoSb.append(Long.toHexString(file.lastModified()) + ":");	//文件创建时间，现在暂时已最后修改时间替代
				additionInfoSb.append(IpMessageConst.IPMSG_FILE_REGULAR + ":");
				byte[] bt = {0x07};		//用于分隔多个发送文件的字符
				String splitStr = new String(bt);
				additionInfoSb.append(splitStr);
			}
			
			sendPro.setAdditionalSection(msgStr + "\0" + additionInfoSb.toString() + "\0");
			
			InetAddress sendto = null;
			try {
				sendto = InetAddress.getByName(receiverIp);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				Log.e("MyFeiGeChatActivity", "发送地址有误");
			}
			if(sendto != null)
				netThreadHelper.sendUdpData(sendPro.getProtocolString(), sendto, IpMessageConst.PORT);
			
			//监听2425端口，准备接受TCP连接请求
			Thread netTcpFileSendThread = new Thread(new NetTcpFileSendThread(filePathArray));
			netTcpFileSendThread.start();	//启动线程
		}
	}
	

}

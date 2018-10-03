package ink.builder.sockettest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class MainActivity extends AppCompatActivity {
    EditText textIP,textPort,textSend,textRadio;//用于跨域访问的控件对象声明
    Button button;//多功能按钮
    String sendData="",radioData="";//收发字符串
    ExecutorService mThreadPool = Executors.newCachedThreadPool();//注册线程池
    Handler mMainHandler=new Handler(){//创建线程到UI的桥接对象
        @Override
        public void handleMessage(Message msg){//接收从线程传来消息并处理
            switch(msg.what){
                case 0:
                    button.setEnabled(false);//防止多次点击
                    button.setText("..连接中..");
                    break;
                case 1:
                    textRadio.setText(radioData=radioData+sendData+"\n");
                    break;
                case 2:
                    textRadio.setText(radioData);
                    button.setEnabled(true);//防止多次点击
                    button.setText(" ..已完成.. ");
                    break;
                case 3:
                    button.setEnabled(true);
                    button.setText(" ..失败 请重试.. ");
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);//载入界面
        textIP = (EditText) findViewById(R.id.textIP);//初始化各控件
        textPort = (EditText) findViewById(R.id.textPort);
        textSend = (EditText) findViewById(R.id.textSend);
        textRadio = (EditText) findViewById(R.id.textRadio);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {//动态创建按钮点击事件
            @Override
            public void onClick(View v) {
                mThreadPool.execute(new Runnable() {//使用线程池管理该新线程
                    @Override
                    public void run() {//重定义本线程运行方法
                        try {
                            Message msg0=Message.obtain();//返回消息对象 用于桥接UI操作
                            msg0.what=0;//定义该消息的标识
                            mMainHandler.sendMessage(msg0);//发送该消息到Handler处理机

                            Socket socket=new Socket(textIP.getText().toString(),Integer.valueOf(textPort.getText().toString()));//只能在非主线程创建连接
                            if ((sendData=textSend.getText().toString()) != "") {//获取发送框数据
                                OutputStream outPut = socket.getOutputStream();
                                outPut.write(sendData.getBytes("UTF-8"));//发送
                                Message msg1 = Message.obtain();//创建消息对象 用于桥接UI操作
                                msg1.what = 1;
                                mMainHandler.sendMessage(msg1);
                            }
                            InputStream inPut = socket.getInputStream();//接收
                            byte[] ss = new byte[512];//需要事先知道服务端传来数据的长度 所以定义数值要大于长度
                            int ssLengh;//不可以在表达式一起，只能提前声明咯
                            if ((ssLengh = inPut.read(ss)) != -1) {//收到的数据存入ss后并返回所收数据的长度，失败返回-1
                                radioData = radioData + new String(ss, 0, ssLengh, "UTF-8")+"\n";//编码成UTF-8
                                Message msg2 = Message.obtain();//创建消息对象 用于桥接UI操作
                                msg2.what = 2;
                                mMainHandler.sendMessage(msg2);
                            }
                            socket.close();//结束Socket
                        } catch (Exception e) {//上面有任何一点错都会跳到这里
                            Message msg3=Message.obtain();
                            msg3.what = 3;
                            mMainHandler.sendMessage(msg3);//创建消息对象 用于桥接UI操作
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
}//结束，，最紧骤的Socket+Handler消息处理模式好像只能这样了。。。耗时三天 暂时不打算深入了

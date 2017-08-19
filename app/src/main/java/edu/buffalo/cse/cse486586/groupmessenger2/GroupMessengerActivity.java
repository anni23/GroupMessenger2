package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.TextView;

import java.util.Iterator;
import java.util.LinkedList;
/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.*;
import java.net.Socket;
import java.net.UnknownHostException;
import edu.buffalo.cse.cse486586.groupmessenger2.R;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import java.net.InetSocketAddress;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;

public class GroupMessengerActivity extends Activity
{
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    EditText editText;
    String myPort;
    TextView localTextView;
    static int counter=0;
    static int ProposedSeqNumber=0;
    static int AgreedSeqNumber=0;
    //String ports[]={"11108","11112","11116","11120","11124"};
    LinkedList<String> ports;
    String failedPort;
    static PriorityQueue<Message> pq;
    static PriorityQueue<Message> pq1;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        Com c=new Com();
        pq=new PriorityQueue<Message>(30,c);
        pq1=new PriorityQueue<Message>(30,c);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        ports=new LinkedList<String>();
        ports.add("11108");
        ports.add("11112");
        ports.add("11116");
        ports.add("11120");
        ports.add("11124");

        try
        {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        editText = (EditText) findViewById(R.id.editText1);
        localTextView = (TextView) findViewById(R.id.textView1);

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(localTextView, getContentResolver()));




    }
    public void sendClicked(View v)
    {

        String msg = editText.getText().toString() + "\n";
        editText.setText(""); // This is one way to reset the input box.
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

    }


    class Message
    {
        int port;
        int id;
        int priority;
        String text;
        String status;


        public boolean equals(Object obj)
        {
            Message m = (Message) obj;

            if ((this.id == m.id))
                {
                    return true;
                }
                else
                {
                    return false;
                }

        }




    }


    class Com implements Comparator<Message>
    {
        public int compare(Message o1, Message o2)
        {
            // TODO Auto-generated method stub

            if (o1.priority <o2.priority)
            {
                return -1;
            }
            if (o1.priority > o2.priority)
            {
                return 1;
            }
            if(o1.priority == o2.priority)
            {
                if (o1.port <= o2.port)
                {
                    return -1;
                }
                if (o1.port > o2.port)
                {
                    return 1;
                }
            }
            return 0;
        }
    }



    private class ServerTask extends AsyncTask<ServerSocket, String, Void>
    {
        ContentValues cv;
        ContentResolver cr;
        Uri.Builder ub;
        Uri uri;
        Socket s1;
        InputStream iss;
        OutputStream oss;
        PrintWriter pws;
        InputStreamReader isrs;
        BufferedReader brs;
        String acks="PA1_OK";
        String string_msg;
        String msgport;
        public int max(int psn,int asn)
        {
            if(psn>asn)
            {
                return psn;
            }
            else
            {
                return asn;
            }
        }
        protected Void doInBackground(ServerSocket... sockets)
        {
            ServerSocket serverSocket = sockets[0];
            ub=new Uri.Builder();
            ub.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
            ub.scheme("content");
            uri=ub.build();
            cr=getContentResolver();
            int key=0;
            String msgid;
            String msgtext;

            String status;
            int pri;


            try
            {
                while(true)
                {
                    serverSocket.setSoTimeout(7500);
                    try {
                        s1 = serverSocket.accept();
                    }
                    catch (SocketTimeoutException e)
                    {
                        Log.i("^^^^^^server side^^^^^","%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");

                        Iterator<Message> it1=pq.iterator();
                        while(it1.hasNext())
                        {
                            Message m2=it1.next();
                            Log.i(TAG,"text="+m2.text);
                            Log.i(TAG,"port="+m2.port);
                            Log.i(TAG,"id="+m2.id);
                            Log.i(TAG,"status="+m2.status);
                            Log.i(TAG,"priority="+m2.priority);

                        }
                        if(pq.peek()!=null) {
                            //if (pq.peek().status.equals("new")) {
                              //  pq.poll();
                            //}
                        }
                        while(pq.peek()!=null)
                        {
                            if(pq.peek().status.equals("deliverable"))
                            {

                                publishProgress(pq.peek().text);


                                cv = new ContentValues();
                                cv.put("key", key);
                                cv.put("value", pq.peek().text);
                                cr.insert(uri, cv);
                                pq.poll();
                                // Log.i(TAG, "key=" + key);
                                Cursor c;
                                c = cr.query(uri, null, key + "", null, null);
                                //Log.i("value from cursor",c.getCount()+"");
                                //Log.i(TAG, "value from cursor=" + c.getString(0));
                                // Log.i(TAG, "value from cursor=" + c.getString(1));

                                key++;
                            }
                            else
                            {
                                break;
                            }
                        }
                    }
                    //s1.setSoTimeout(1200);
                    iss = s1.getInputStream();
                    isrs = new InputStreamReader(iss);
                    brs = new BufferedReader(isrs);
                    string_msg = brs.readLine();
                    oss=s1.getOutputStream();
                    pws=new PrintWriter(oss);

                    int a=string_msg.indexOf(",,");

                    status=string_msg.substring(0,a);
                    String temp=string_msg.substring(a+2,string_msg.length());

                    a=temp.indexOf(",,");
                    msgport=temp.substring(0,a);
                    String temp1=temp.substring(a+2,temp.length());

                    a=temp1.indexOf(",,");
                    msgid=temp1.substring(0,a);
                    msgtext=temp1.substring(a+2,temp1.length());


                    if(status.equals("new"))
                    {

                        ProposedSeqNumber=max(ProposedSeqNumber,AgreedSeqNumber);
                        ProposedSeqNumber++;
                      //  Log.i(TAG,"proposed seq no ="+ProposedSeqNumber);
                        Message m=new Message();
                        m.id=Integer.parseInt(msgid);
                        m.port=Integer.parseInt(msgport);
                        m.priority=ProposedSeqNumber;
                        m.text=msgtext;
                        m.status=status;
                      //  Log.i(TAG,"-----------------------------------------------");
                        //put the msg in queue
                        //synchronized (this)
                        //{

                        Log.i(TAG,"queue size ="+pq.size());

                            pq.add(m);
                        //}
                        //pq.add(m);



                        //Log.i(TAG,"add in queue="+pq.add(m));
                        //increment your global sequence number

                        //send proposed seq no to the sender of the msg
/*                        Iterator<Message> it1=pq.iterator();
                      //  Log.i(TAG,"queue in first server loop =");
                        while(it1.hasNext())
                        {
                            Message m2=it1.next();
                            Log.i(TAG,"text="+m2.text);
                            Log.i(TAG,"port="+m2.port);
                            Log.i(TAG,"id="+m2.id);
                            Log.i(TAG,"status="+m2.status);
                            Log.i(TAG,"priority="+m2.priority);

                        }*/

                        pws.write(acks);
                        pws.write(",,");
                        pws.write(ProposedSeqNumber+"");
                        pws.flush();

                        pws.close();
                        brs.close();
                        s1.close();
                        /*Log.i(TAG,"-----------------------------------------------");

                        Log.i(TAG,"first time msg="+m.port);
                        Log.i(TAG,"first time msg="+m.priority);
                        Log.i(TAG,"first time msg="+m.text);
                        Log.i(TAG,"first time msg="+m.status);
                        Log.i(TAG,"first time msg="+m.id);
                        Log.i(TAG,*/
                    }
                    else
                    {
                        Message m1=new Message();
                        m1.text=msgtext;
                        m1.port=Integer.parseInt(msgport);
                        m1.priority=Integer.parseInt(status);
                        m1.status="deliverable";
                        m1.id=Integer.parseInt(msgid);


                      /*  Log.i(TAG,"second time msg="+m1.port);
                        Log.i(TAG,"second time msg="+m1.priority);
                        Log.i(TAG,"second time msg="+m1.text);
                        Log.i(TAG,"second time msg="+m1.status);
                        Log.i(TAG,"second time msg="+m1.id);
                        Log.i(TAG,"-----------------------------------------------");*/
                        //synchronized (this)
                        //{
                            pq.remove(m1);
                            pq.add(m1);


                        //}

                        //pq.remove(m1);
                        //pq.add(m1);
                        AgreedSeqNumber=Integer.parseInt(status);

                        //find the msg in the queue
                        /*
                        for(int i=0;i<pl.size();i++)
                        {
                            if(pl.get(i).id==Integer.parseInt(msgid))
                            {
                                pl.get(i).priority=pri;
                                pq.add(pl.get(i));
                                Log.i(TAG,"second time msg="+pl.get(i).port);
                                Log.i(TAG,"second time msg="+pl.get(i).priority);
                                Log.i(TAG,"second time msg="+pl.get(i).text);
                                break;
                            }
                        }
                        */
                        //update its seq no
/*
                        Iterator<Message> it1=pq.iterator();
                        Log.i(TAG,"queue in second server loop =");
                        while(it1.hasNext())
                        {
                            Message m2=it1.next();
                            Log.i(TAG,"text="+m2.text);
                            Log.i(TAG,"port="+m2.port);
                            Log.i(TAG,"id="+m2.id);
                            Log.i(TAG,"status="+m2.status);
                            Log.i(TAG,"priority="+m2.priority);

                        }*/



                        pws.write(acks);
                        pws.flush();
                        pws.close();
                        brs.close();
                        s1.close();
                       // Log.i(TAG,"-----------------------------------------------");

                    }


                    if(pq.size()>=8) {
                        while (pq.peek() != null) {
                            if (pq.peek().status.equals("deliverable")) {

                                publishProgress(pq.peek().text);


                                cv = new ContentValues();
                                cv.put("key", key);
                                cv.put("value", pq.peek().text);
                                cr.insert(uri, cv);
                                pq.poll();
                                // Log.i(TAG, "key=" + key);
                                Cursor c;
                                c = cr.query(uri, null, key + "", null, null);
                                //Log.i("value from cursor",c.getCount()+"");
                                //Log.i(TAG, "value from cursor=" + c.getString(0));
                                // Log.i(TAG, "value from cursor=" + c.getString(1));

                                key++;
                            } else {
                                break;
                            }
                        }

                    }



                }

            }
            catch(IOException e)
            {
                Log.e(TAG,"server side failure"+msgport);
                //e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String...strings)
        {

            String strReceived = strings[0].trim();
            localTextView.append(strReceived + "\t\n");


            try {
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }
    private class ClientTask extends AsyncTask<String, Void, Void>
    {
        OutputStream osc;
        PrintWriter pwc;
        InputStream isc;
        InputStreamReader isrc;
        BufferedReader brc;
        String remotePort;
        Socket socket;
        String myport;
        String msgToSend;
        String msgtext;
        String msgid;
        protected Void doInBackground(String... msgs) {
            try {
                myport=msgs[1];
                counter++;
                msgid=myport+counter;
                msgtext = msgs[0];
                String status="new";
                msgToSend=status+",,"+myport+",,"+msgid+",,"+msgtext;
                LinkedList<Integer> proposedseqnos;
                proposedseqnos=new LinkedList<Integer>();
                //Log.i(TAG,"client called="+counter);
                //Log.i(TAG,"my port="+myport);
                for(int i=0;i<ports.size();i++)
                {
                    remotePort = ports.get(i);
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                   // socket.setSoTimeout(7000);
                    String ackc = "PA1_OK";
                    osc = socket.getOutputStream();
                    pwc = new PrintWriter(osc);
                    //Log.i(TAG,"message ="+msgToSend);
                    pwc.write(msgToSend);
                    pwc.flush();
                    isc = socket.getInputStream();
                    isrc = new InputStreamReader(isc);
                    brc = new BufferedReader(isrc);
                    String x = brc.readLine();
                    if(x==null)
                    {


                        failedPort=remotePort;
                        Log.i(TAG,"failed port in loop 1="+remotePort);
                        //if(ports.contains(remotePort))
                        //ports.remove(remotePort);
                        //Log.i(TAG,"ports="+ports);


                        Iterator<Message> it=pq.iterator();
                        while(it.hasNext())
                        {
                            Message m1=it.next();
                            Log.i(TAG,"text="+m1.text);
                            Log.i(TAG,"port="+m1.port);
                            Log.i(TAG,"id="+m1.id);
                            Log.i(TAG,"status="+m1.status);
                            Log.i(TAG,"priority="+m1.priority);

                            if((m1.port==Integer.parseInt(failedPort))&&(m1.status.equals("new")))
                            {
                                    it.remove();
                                Log.i(TAG,"removed###########################################");
                            }


                        }


                        /*for(int j=1;j<=25;j++)
                        {
                            String id=failedPort+j;
                            Message m=new Message();
                            m.id=Integer.parseInt(id);
                            m.text="failure";
                            m.port=Integer.parseInt(failedPort);
                            //Log.i(TAG,"failure handling="+m.id);
                            //Log.i(TAG,"failure handling="+m.text);
                            //Log.i(TAG,"failure handling="+m.port);
                            Log.i(TAG,"failure handling="+pq.contains(m));
                        }*/

                        Log.i(TAG,"...............................................");

                        Iterator<Message> it1=pq.iterator();
                        while(it1.hasNext())
                        {
                            Message m2=it1.next();
                            Log.i(TAG,"text="+m2.text);
                            Log.i(TAG,"port="+m2.port);
                            Log.i(TAG,"id="+m2.id);
                            Log.i(TAG,"status="+m2.status);
                            Log.i(TAG,"priority="+m2.priority);

                        }


                    }
                    else {
                            int a = x.indexOf(",,");
                            String x1 = (x.substring(0, a));
                            //Log.i(TAG,"proposed seq no="+Integer.parseInt(x.substring(a + 2, x.length())));
                            proposedseqnos.add(Integer.parseInt(x.substring(a + 2, x.length())));
                            if (x1.equals(ackc)) {
                                pwc.close();
                                brc.close();
                                socket.close();
                            }
                    }

                }

                Collections.sort(proposedseqnos);
                String priority;
                priority=proposedseqnos.getLast()+"";
                //Log.i(TAG,"agreed seq no ="+priority);
                msgToSend=priority+",,"+myport+",,"+msgid+",,"+msgtext;

                for(int i=0;i<ports.size();i++)
                {
                    remotePort = ports.get(i);
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                   // socket.setSoTimeout(7000);
                    String ackc = "PA1_OK";
                    osc = socket.getOutputStream();
                    pwc = new PrintWriter(osc);
                    //Log.i(TAG,"message with agreed seq no ="+msgToSend);
                    pwc.write(msgToSend);
                    pwc.flush();
                    isc = socket.getInputStream();
                    isrc = new InputStreamReader(isc);
                    brc = new BufferedReader(isrc);
                    String x = brc.readLine();
                    if(x==null)
                    {

                        failedPort=remotePort;
                        Log.i(TAG,"failed port in loop 2="+remotePort);
                        //if(ports.contains(remotePort))
                          //  ports.remove(remotePort);
                        //Log.i(TAG,"ports="+ports);


                        Iterator<Message> it=pq.iterator();
                        while(it.hasNext())
                        {
                            Message m1=it.next();
                            Log.i(TAG,"text="+m1.text);
                            Log.i(TAG,"port="+m1.port);
                            Log.i(TAG,"id="+m1.id);
                            Log.i(TAG,"status="+m1.status);
                            Log.i(TAG,"priority="+m1.priority);

                            if((m1.port==Integer.parseInt(failedPort))&&(m1.status.equals("new")))
                            {
                                it.remove();
                                Log.i(TAG,"removed###########################################");
                            }


                        }

                        Log.i(TAG,"...............................................");

                        Iterator<Message> it1=pq.iterator();
                        while(it1.hasNext())
                        {
                            Message m2=it1.next();
                            Log.i(TAG,"text="+m2.text);
                            Log.i(TAG,"port="+m2.port);
                            Log.i(TAG,"id="+m2.id);
                            Log.i(TAG,"status="+m2.status);
                            Log.i(TAG,"priority="+m2.priority);

                        }

                    }
                    else {
                            if (x.equals(ackc)) {
                                pwc.close();
                                brc.close();
                                socket.close();
                            }
                    }
                }
            }
            catch (UnknownHostException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                Log.i("client side","%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
                e.printStackTrace();
            }

            return null;
        }
    }
}






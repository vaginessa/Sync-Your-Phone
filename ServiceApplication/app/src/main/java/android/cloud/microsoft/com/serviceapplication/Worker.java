package android.cloud.microsoft.com.serviceapplication;

import android.os.Environment;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

//this implements the actual transfer of data

public class Worker implements Runnable
{

    private ServerSocket serverSocket=null;
    private Socket clientSocket=null;
    private BufferedReader readMetaData=null;
    private BufferedInputStream br=null;
    private PrintWriter out=null;
    private BufferedOutputStream bufferOut;

    private boolean terminateFlag = false;
    private boolean isAvailableServiceObj = false;

    private String appStatus;

    MyService myServiceObj;


    public Worker(MyService obj)
    {
        myServiceObj = obj;
    }

    public String getStatus()
    {
        return appStatus;
    }

    public void setTerminateFlag()
    {
        terminateFlag = true;
    }

    public void checkServiceObj()
    {
        isAvailableServiceObj = true;
    }


    public void receiveData(ServerSocket serverSocket,File musicFilePath) throws Exception
    {



        while(!(terminateFlag)) {

            appStatus = "Connected to Phone";

            if(isAvailableServiceObj)
            myServiceObj.display("Connected to Phone!");

            try {


                Log.i("Log", "Waiting for Client connection...");
                clientSocket = serverSocket.accept();
                Log.i("Log", "Connected to Client...");
                Log.i("Log", "Receiving Meta Data");
                appStatus = "Receiving Data";
                if(isAvailableServiceObj)
                    myServiceObj.display("Receiving Data");

                readMetaData = new BufferedReader((new InputStreamReader(clientSocket.getInputStream())));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                br = new BufferedInputStream(clientSocket.getInputStream());


                String rawData = null;
                while ((rawData = readMetaData.readLine()) != null) {

//                    myServiceObj.display("I am receiving");

                    JSONObject metaData = new JSONObject(rawData);
                    Log.i("MetaDataContent", rawData);

                    String mp3Name = metaData.getString("mp3Name");
                    long mp3Size = Long.parseLong(metaData.getString("mp3Size"));
                    String dirName = metaData.getString("dirName");

                    out.println("Server:Meta Data receieved");

                    Log.i("Log", "Receiving File");

                    //Creating directory first
                    File dirN= new File(musicFilePath,dirName);
                    dirN.mkdirs();

                    if(mp3Size != 0)
                    {
                        //creating file object
                        File mp3File = new File(musicFilePath, dirName+mp3Name);


                        bufferOut = new BufferedOutputStream(new FileOutputStream(mp3File));


                        byte[] buffer = new byte[1024 * 1024];

                        // Looping till we reach the end of file i.e value returned is -1 which is when socket gets closed
                        int count = 0;
                        int recv = 0;

                        while (recv < mp3Size && (count = br.read(buffer)) > 0) {

                            bufferOut.write(buffer, 0, count);

                            recv += count;
                            Log.i("Receiving", "Data" + recv + "\n");


                        }
                        bufferOut.flush();
                    }
                    out.println("Server: File/Folder Receive\n");

                    Log.i("Log", "Received Successfully");


                }

                myServiceObj.refreshWindow();
                br.close();
                out.close();
                readMetaData.close();
                bufferOut.close();

            }
            catch (Exception e)
            {
                appStatus = "Disconnected";
                if(isAvailableServiceObj)
                    myServiceObj.display("Disconnected");
                e.printStackTrace();
                Log.e("ErrorLog", "" + e.getMessage());
                Log.i("Log", "Here");

            }

            finally
            {
                // deallocating resources

                readMetaData = null;
                br = null;
                out = null;
                bufferOut = null;

            }
        }



    }


    @Override
    public void run()  {

        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {

            //This provides the path of Music Directory as given in my phone. emulated/0/music/
            final File musicFilePath = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC);

            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
//                    serverSocket.setSoTimeout(10000);
                serverSocket.bind(new InetSocketAddress(9998));
                receiveData(serverSocket,musicFilePath);
            }
            catch(Exception e)
            {

                e.printStackTrace();
                Log.e("ErrorLog", "" + e.getMessage());
                Log.i("Log", "Here");


            }

            finally{
                // deallocating resources
                try {
                    clientSocket.close();
                    serverSocket.close();
                    readMetaData = null;
                    br = null;
                    out = null;
                    bufferOut = null;

                } catch (Exception e) {
                    String error = e.getMessage();
                    if (error != null)
                        Log.i("Try in Try", error);
                }

            }


        }
        else Log.i("Status","SD card not available");
    }


    public void clearResources()
    {
        appStatus = "Disconnected";
        if(isAvailableServiceObj)
            myServiceObj.display("Disconnected");

        readMetaData=null;
        br=null;
        out=null;
        bufferOut=null;

        try {
            if(clientSocket!=null&&clientSocket.isConnected())
                clientSocket.close();
            else
            {
                clientSocket=null;
            }

            serverSocket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            String error=e.toString();
            if(error!=null)
                Log.i("Try in Try2",error);
        }

    }

}

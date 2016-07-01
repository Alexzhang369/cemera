package it.sauronsoftware.ftp4j;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String host="www.t8go.com";
        String user="archko";
        String pass="331337913";

        FTPClient client=new FTPClient();
        try {
            client.connect(host, 2001);
            client.login(user, pass, null);

            FTPFile[] list=client.list();
            for (FTPFile file : list) {
                System.out.println("file:"+file);
            }

            transFile(client);
            //client.createDirectory("newDir");
            //client.deleteDirectory("newDir");
            client.disconnect(true);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FTPIllegalReplyException e) {
            e.printStackTrace();
        } catch (FTPException e) {
            e.printStackTrace();
        } catch (FTPDataTransferException e) {
            e.printStackTrace();
        } catch (FTPAbortedException e) {
            e.printStackTrace();
        } catch (FTPListParseException e) {
            e.printStackTrace();
        }
    }

    private static void transFile(FTPClient client) throws IllegalStateException,
        FileNotFoundException, IOException,
        FTPIllegalReplyException, FTPException,
        FTPDataTransferException, FTPAbortedException {
        FTPDataTransferListener listener=new FTPDataTransferListener() {

            @Override
            public void started() {
                System.out.println("started");
            }

            @Override
            public void started(long totalSize) {
                System.out.println("transferred.totalSize:"+totalSize);
            }

            @Override
            public void transferred(int length) {
            }

            @Override
            public void transferred(long length) {
                System.out.println("transferred.length:"+length);
            }

            @Override
            public void completed() {
                System.out.println("completed");
            }

            @Override
            public void aborted() {
                System.out.println("aborted");
            }

            @Override
            public void failed() {
                System.out.println("failed");
            }

        };

        //client.upload(new File("k:\\非我莫属.f4v"),listener);		
    }

}

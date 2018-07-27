package com.ssh.agent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;

public class SSHAgent implements Logger {

   
   private String ip = "192.168.0.183";
   private int port = 22;
   private String id;
   private String password;
   private String privateKeyPath;
   private String privateKeyPassWord;
   private String propertiesPath;
   
   private JSch jsch;
   private Session session;
   private Channel channel;
   private Properties properties;
   
   public SSHAgent(String id, String password) {
       this.id = id;
       this.password = password;
       JSch.setLogger(this);
   }
   
   public SSHAgent(String id, String privateKeyPath, String privateKeyPassWord) {
       this.id = id;
       this.privateKeyPath = privateKeyPath;
       this.privateKeyPassWord = privateKeyPassWord;
       JSch.setLogger(this);
   }
   
   public void setProperties(String propertiesPath) throws Exception {
       this.propertiesPath =  propertiesPath;
       File file = new File(propertiesPath);
       if (!file.exists() || !file.isFile()) {
           throw new FileNotFoundException("Check properties file path : " + propertiesPath);
       }
       
       properties = new Properties();
       FileInputStream in = null;
       try {
           in = new FileInputStream(file);
           properties.load(in);
       } catch (IOException e) {
           System.out.println("While loading properties, error occurred!");
           throw e;
       } finally {
           if (in != null) {
               try {
                   in.close();
               } catch (IOException ex){}
           }
       }
   }
   
   private void printConnInfo() {
       StringBuffer sb = new StringBuffer();
       sb.append("Connection info - ");
       if (password != null) {
           sb.append("ip : " + ip + ", port : " + port + ", password : " + password);
           if (propertiesPath != null) {
               sb.append(", read properties file : " + propertiesPath);
           }
       } else {
           sb.append("ip : " + ip + ", port : " + port + ", private key path : " + privateKeyPath + ", private key pass phrase : " + privateKeyPassWord);
           if (propertiesPath != null) {
               sb.append(", read properties file : " + propertiesPath);
           }
       }
       System.out.println(sb.toString());
   }
   
   private void connect() throws Exception {
       printConnInfo();
       System.out.println("Start connect");
       jsch = new JSch();
       
       try {
           if (password != null) {
               session = jsch.getSession(id, ip, port);
               session.setPassword(password);
               if (properties != null) {
                   session.setConfig(properties);
               }
               session.connect(10 * 1000);
           } else {
               jsch.addIdentity(privateKeyPath, privateKeyPassWord);
               session = jsch.getSession(id,ip, port);
               if (properties != null) {
                   session.setConfig(properties);
               }
               session.connect(10 * 1000); 
           }
           System.out.println("Client : " + session.getClientVersion() + ", Server : " + session.getServerVersion());
       } catch (Exception e) {
           System.out.println("When connecting using ssh, error occured");
           e.printStackTrace();
           throw e;
       }
   }
   
   public void sshCommandTest() {
       InputStream in = null;
       InputStream err = null;
       try {
           connect();
           channel = session.openChannel("exec");
           ChannelExec exec = (ChannelExec) channel;
           System.out.println("Connect host : " + ip + ", port : " + port +", id : " + id);
           System.out.println("Test command : echo AAA");
           exec.setCommand("echo AAA");
           exec.connect();
           
           ByteArrayOutputStream baos = new ByteArrayOutputStream();
           ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
           in = exec.getInputStream();
           err = exec.getErrStream();
           
           byte[] buffer = new byte[1024];
           byte[] errBuffer = new byte[1024];
           
           while(true) {
               int len = in.read(buffer);
               if (len < 0) {
                   break;
               }
               baos.write(buffer, 0, len);
           }
           
           while(true) {
               int len = err.read(errBuffer);
               if (len < 0) {
                   break;
               }
               errBaos.write(errBuffer, 0, len);
           }
           System.out.println("Command result");
           System.out.println("STD OUT : " + new String(baos.toByteArray()));
           System.out.println("STD ERR : " + new String(errBaos.toByteArray()));
           System.out.println("Success command test");
       } catch (Exception e) {
           System.out.println("When ssh command testing, error occured");
           e.printStackTrace();
       } finally {
           if (in != null) {
               try {
                in.close();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
           if ( channel != null) {
               channel.disconnect();
           }
           if (session != null) {
               session.disconnect();
           }
       }
   }
   
   public static void main(String[] args) {
       SSHAgent agent = null;
       boolean initAgent = false;
       
       try {
           if (args.length == 2) {
               agent = new SSHAgent(args[0], args[1]);
               initAgent = true;
           } else if (args.length == 3) {
               File file = new File(args[2]);
               if (file.exists() && file.isFile()) {
                   agent = new SSHAgent(args[0], args[1]);
                   agent.setProperties(args[2]);
               } else {
                   agent = new SSHAgent(args[0], args[1], args[2]);
               }
               initAgent = true;
           } else if (args.length == 4) {
               agent = new SSHAgent(args[0], args[1], args[2]);
               agent.setProperties(args[3]);
               initAgent = true;
           } else {
               System.out.println("Commnad Help : ");
               System.out.println("java -jar ssh_test.jar <id> <password> [properties path]");
               System.out.println("-------------------------------- OR -----------------------------------------------------");
               System.out.println("java -jar ssh_test.jar <id> <private key path> <private key passphrase> [properties path]");
           }
       } catch (Exception e) {
           System.out.println("ssh test failed!");
           e.printStackTrace();
       }
       
       if (initAgent) {
           try {
            agent.sshCommandTest();
        } catch (Exception e) {
            System.out.println("ssh test failed !");
            e.printStackTrace();
        }
       }
   }

   @Override
   public boolean isEnabled(int level) {
       return true;
   }
    
   @Override
   public void log(int level, String message) {
       System.out.println("[" + level + "] " + message);
   }
}

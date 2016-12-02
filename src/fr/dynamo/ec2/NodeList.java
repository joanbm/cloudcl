package fr.dynamo.ec2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.opencl.OpenCLPlatform;

public class NodeList {

  private final String nodeFilePath;
  private Set<DynamoInstance> nodes = new HashSet<DynamoInstance>();

  private static NodeList instance;

  private NodeList() throws IOException{
    if(System.getenv().containsKey("DCL_NODE_FILE")){
      nodeFilePath = System.getenv().get("DCL_NODE_FILE");
    }else{
      throw new IOException("Environment Variable for DCL_NODE_FILE not defined.");
    }
    serialize();
  }

  public static NodeList getInstance(){
    if(instance == null)
      try {
        instance = new NodeList();
      } catch (IOException e) {
        e.printStackTrace();
      }

    return instance;
  }

  public void addNode(DynamoInstance node){
    synchronized(nodes){
      nodes.add(node);
      node.setDevices(getDevicesForNode(node));
      serialize();
    }
  }

  public void removeNode(DynamoInstance node){
    synchronized(nodes){
      nodes.remove(node);
      serialize();
    }
  }

  public Set<DynamoInstance> getNodes(){
    synchronized(nodes){
      return nodes;
    }
  }

  public Set<OpenCLDevice> getAllDevices(){
    Set<OpenCLDevice> devices = new HashSet<OpenCLDevice>();
    synchronized(nodes){
      for (OpenCLPlatform platform : OpenCLPlatform.getUncachedOpenCLPlatforms()) {
        for (OpenCLDevice device : platform.getOpenCLDevices()) {
          devices.add(device);
        }
      }
    }
    return devices;
  }

  private Set<OpenCLDevice> getDevicesForNode(DynamoInstance node){
    Set<OpenCLDevice> devices = new HashSet<OpenCLDevice>();

    serializeSingleNode(node);

    for (OpenCLPlatform platform : OpenCLPlatform.getUncachedOpenCLPlatforms()) {
      for (OpenCLDevice device : platform.getOpenCLDevices()) {
        devices.add(device);
      }
    }

    return devices;
  }

  private void serialize(){
    StringBuffer buffer = new StringBuffer();

    for(DynamoInstance node:nodes){
      buffer.append(node.getPublicIp()+"\n");
    }

    try {
      Files.write(Paths.get(nodeFilePath), buffer.toString().getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private void serializeSingleNode(DynamoInstance node){
    try {
      Files.write(Paths.get(nodeFilePath), node.getPublicIp().getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}

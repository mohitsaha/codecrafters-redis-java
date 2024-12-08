package replication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;

import common.SocketUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import redis.RedisResultData;

@Slf4j
@Getter
@Setter
public class MasterConnectionProvider {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean isAckRequested = false;
    private boolean isConfirmed = true;
    private int desiredAck = 0;
    private int presentAck = 0;
    public static final int REPLCONF_ACK_BYTE_SIZE = 37;

    public MasterConnectionProvider(Socket socket, BufferedWriter writer) {
        try {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = writer;
        } catch (IOException e) {

        }
    }

    public void sendMessage(List<String> param) {
        try {
            var arrayData = RedisResultData.getArrayData(param.toArray(new String[0]));
            var message = RedisResultData.convertToOutputString(arrayData);

            SocketUtil.sendStringToSocket(writer, message);
            desiredAck += message.length();
        } catch (IOException e) {
            log.error("IOException", e);
        }
    }

    public void sendAck() {
        try {
            var ackRequest = RedisResultData.getArrayData("REPLCONF", "GETACK", "*");
            var message = RedisResultData.convertToOutputString(ackRequest);

            SocketUtil.sendStringToSocket(writer, message);
            desiredAck += message.length();
            isAckRequested = true;
        } catch (IOException e) {
            log.error("IOException", e);
        }
    }

    public boolean isFullySynced() {
        if (desiredAck - REPLCONF_ACK_BYTE_SIZE <= presentAck) {
            isConfirmed = true;
        }

        return desiredAck - REPLCONF_ACK_BYTE_SIZE <= presentAck;
    }
}
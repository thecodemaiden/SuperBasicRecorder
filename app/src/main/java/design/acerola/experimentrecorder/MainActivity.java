package design.acerola.experimentrecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int RECORDER_SAMPLERATE = 48000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    int BufferElements2Rec = 2048; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    int fileIdx = 1;


    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            if (v.getId() == R.id.btnToggle) {
                Button b = (Button)v;
                if (!isRecording) {
                    b.setText("Recording");
                    startRecording();
                } else{
                    stopRecording();
                    b.setText("Record");
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button toggleBtn = findViewById(R.id.btnToggle);
        toggleBtn.setOnClickListener(btnClick);
        toggleBtn.setEnabled(true);

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        if (bufferSize > BufferElements2Rec*BytesPerElement) {
            BufferElements2Rec = bufferSize/BytesPerElement;
        }

    }

    private String getDateSuffix(){
        Date now = Calendar.getInstance().getTime();
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        String dateString = df.format(now);
        return  dateString;
    }

    private void startRecording() {
    int bufferSize = BufferElements2Rec * BytesPerElement;
        Log.d("AudioRecord", "Starting to record with sample rate: "+RECORDER_SAMPLERATE+
                 "buffer: "+bufferSize);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);


        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecord", "Cannot record like this");
        }

        final String fileName = ((EditText)findViewById(R.id.filePrefix)).getText().toString()
                + getDateSuffix() +"-" + String.valueOf(fileIdx)+".dat";
        fileIdx = fileIdx+1;

        ((TextView) findViewById(R.id.txtV2)).setText(fileName);
        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile(fileName);
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile(String fileName) {
        // Write the output audio in byte

        String saveDirPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "experiment1";
       File saveDir = new File(saveDirPath);
       boolean success = true;
       if (!saveDir.exists()) {
            success = saveDir.mkdirs();
       }

       if (!success) {
           // TODO Handle error properly
           Log.e("AudioRecord", "Can't make my folder!");
           return;
       }
       Log.d("AudioRecord", "Saving to"+saveDirPath);
        File saveFile = new File(saveDirPath+File.separator+fileName);

        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(saveFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BufferElements2Rec);
            //System.out.println("Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


}

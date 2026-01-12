package com.example.lims_v3.util;

import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeliCaReader {

    private static final String TAG = "FeliCaReader";
    private static final String ERROR_TAG = "FeliCaReaderError";

    // --- FeliCa コマンド関連の定数 ---
    private static final byte COMMAND_READ_WITHOUT_ENCRYPTION = 0x06;
    private static final int SERVICE_CODE_STUDENT_ID = 0x010B;

    // --- 解析関連の定数 ---
    private static final int RESPONSE_HEADER_SIZE = 13;
    private static final int RESPONSE_STATUS_FLAG_INDEX = 10;
    private static final byte RESPONSE_STATUS_OK = 0x00;
    private static final int STUDENT_ID_START_INDEX = 3;
    private static final int STUDENT_ID_END_INDEX = 10;
    private static final String CHARSET_SHIFT_JIS = "Shift_JIS";

    // 非同期処理のためのExecutor
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 読み取り結果を通知するためのコールバックインターフェース
     */
    public interface FeliCaCallback {
        void onSuccess(@NonNull String studentId);
        void onFailure(@NonNull Exception exception);
    }

    /**
     * FeliCaタグから非同期でデータを読み取ります。
     *
     * @param tag      NFCタグ
     * @param callback 結果を返すコールバック
     */
    public void readStudentId(@NonNull Tag tag, @NonNull FeliCaCallback callback) {
        executor.execute(() -> {
            NfcF nfcF = NfcF.get(tag);
            if (nfcF == null) {
                callback.onFailure(new IOException("Tag does not support NfcF."));
                return;
            }

            try {
                nfcF.connect();
                Log.d(TAG, "NFC connected.");

                // コマンドを生成して実行
                byte[] command = buildReadCommand(nfcF.getSystemCode(), nfcF.getTag().getId());
                byte[] response = nfcF.transceive(command);
                Log.d(TAG, "Response: " + bytesToHex(response));

                // レスポンスを解析
                String studentId = parseStudentIdFromResponse(response);
                callback.onSuccess(studentId);

            } catch (Exception e) {
                Log.e(ERROR_TAG, "NFC communication error: " + e.getMessage(), e);
                callback.onFailure(e);
            } finally {
                try {
                    nfcF.close();
                    Log.d(TAG, "NFC closed.");
                } catch (IOException e) {
                    Log.e(ERROR_TAG, "NFC close error.", e);
                }
            }
        });
    }

    /**
     * FeliCaの暗号化なし読み取りコマンドを生成します。
     */
    private byte[] buildReadCommand(byte[] systemCode, byte[] idm) {
        byte[] serviceCodeLE = {
                (byte) (SERVICE_CODE_STUDENT_ID & 0xFF),
                (byte) ((SERVICE_CODE_STUDENT_ID >> 8) & 0xFF)
        };

        // コマンドフォーマット: [コマンド長][コマンドコード][IDm...][サービス数][サービスコード...][ブロック数][ブロックリスト...]
        byte[] command = new byte[2 + idm.length + 1 + serviceCodeLE.length + 1 + 2];
        int pos = 0;
        command[pos++] = (byte) command.length;
        command[pos++] = COMMAND_READ_WITHOUT_ENCRYPTION;
        System.arraycopy(idm, 0, command, pos, idm.length);
        pos += idm.length;
        command[pos++] = 1; // サービス数
        System.arraycopy(serviceCodeLE, 0, command, pos, serviceCodeLE.length);
        pos += serviceCodeLE.length;
        command[pos++] = 1; // ブロック数
        command[pos++] = (byte) 0x80; // ブロック要素 (2byte/block, 先頭ブロック)
        command[pos]   = 0; // ブロック番号

        Log.d(TAG, "Command: " + bytesToHex(command));
        return command;
    }

    /**
     * FeliCaの応答データから学籍番号を抽出します。
     */
    private String parseStudentIdFromResponse(byte[] response) throws IOException, UnsupportedEncodingException {
        if (response == null || response.length < RESPONSE_HEADER_SIZE || response[RESPONSE_STATUS_FLAG_INDEX] != RESPONSE_STATUS_OK) {
            throw new IOException("Invalid or error response from FeliCa card. Response: " + bytesToHex(response));
        }

        byte[] data = Arrays.copyOfRange(response, RESPONSE_HEADER_SIZE, response.length);
        String decodedString = new String(data, CHARSET_SHIFT_JIS).trim();
        Log.d(TAG, "Decoded raw data: " + decodedString);

        if (decodedString.length() < STUDENT_ID_END_INDEX) {
            throw new IOException("Response data is too short.");
        }

        // 特定の範囲を学籍番号として抽出
        return decodedString.substring(STUDENT_ID_START_INDEX, STUDENT_ID_END_INDEX);
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}
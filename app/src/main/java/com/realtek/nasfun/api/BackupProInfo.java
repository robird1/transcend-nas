package com.realtek.nasfun.api;

public class BackupProInfo{
    int status_code;
    int progress;

    public BackupProInfo(int status_code, int progress){
        this.status_code = status_code;
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getStatus_code() {
        return status_code;
    }

    public void setStatus_code(int status_code) {
        this.status_code = status_code;
    }

}

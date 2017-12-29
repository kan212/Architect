package com.architect.base.api;

/**
 * Created by kan212 on 17/12/28.
 */

public class TestBean {


    public TestResult result;

    public class TestResult {
        public Status status;

        public TransforRoom data;

        public TransforRoom getData() {
            return data;
        }

        public void setData(TransforRoom data) {
            this.data = data;
        }
    }

    public class Status{
        public int code;
        public String msg;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }


    public class TransforRoom {
        public String name;

        public String room_id;

        public String increate_id;

        public String match_id;

        public String discip;

        public String league;

        public String data_from;

        public String notice;

        public String start_time;

        public String end_time;

        public String ctime;

        public String mtime;

        public String status;

        public String note;

        public String note_url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRoom_id() {
            return room_id;
        }

        public void setRoom_id(String room_id) {
            this.room_id = room_id;
        }

        public String getIncreate_id() {
            return increate_id;
        }

        public void setIncreate_id(String increate_id) {
            this.increate_id = increate_id;
        }

        public String getMatch_id() {
            return match_id;
        }

        public void setMatch_id(String match_id) {
            this.match_id = match_id;
        }

        public String getDiscip() {
            return discip;
        }

        public void setDiscip(String discip) {
            this.discip = discip;
        }

        public String getLeague() {
            return league;
        }

        public void setLeague(String league) {
            this.league = league;
        }

        public String getData_from() {
            return data_from;
        }

        public void setData_from(String data_from) {
            this.data_from = data_from;
        }

        public String getNotice() {
            return notice;
        }

        public void setNotice(String notice) {
            this.notice = notice;
        }

        public String getStart_time() {
            return start_time;
        }

        public void setStart_time(String start_time) {
            this.start_time = start_time;
        }

        public String getEnd_time() {
            return end_time;
        }

        public void setEnd_time(String end_time) {
            this.end_time = end_time;
        }

        public String getCtime() {
            return ctime;
        }

        public void setCtime(String ctime) {
            this.ctime = ctime;
        }

        public String getMtime() {
            return mtime;
        }

        public void setMtime(String mtime) {
            this.mtime = mtime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public String getNote_url() {
            return note_url;
        }

        public void setNote_url(String note_url) {
            this.note_url = note_url;
        }
    }
}

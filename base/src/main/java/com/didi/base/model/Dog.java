package com.didi.base.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by pngfi on 2017/9/26.
 */

public class Dog implements Parcelable{

    private String name;

    public Dog(String name) {
        this.name = name;
    }

    protected Dog(Parcel in) {
        name = in.readString();
    }

    public static final Creator<Dog> CREATOR = new Creator<Dog>() {
        @Override
        public Dog createFromParcel(Parcel in) {
            return new Dog(in);
        }

        @Override
        public Dog[] newArray(int size) {
            return new Dog[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
    }
}

package com.didi.virtualapk.demo.binderpool;

interface ISecurityCenter {
    String encrypt(String content);
    String decrypt(String password);
}
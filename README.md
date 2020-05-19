# 永不丢失的EditText

![image](https://img.shields.io/badge/build-passing-brightgreen.svg) [![GitHub license](https://img.shields.io/github/license/zsqw123/Non-Lose-EditText)](https://github.com/zsqw123/Non-Lose-EditText/blob/master/LICENSE) [![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/zsqw123/Non-Lose-EditText) ![GitHub All Releases](https://img.shields.io/github/downloads/zsqw123/Non-Lose-EditText/total) [![GitHub stars](https://img.shields.io/github/stars/zsqw123/Non-Lose-EditText)](https://github.com/zsqw123/Non-Lose-EditText/stargazers) [![GitHub forks](https://img.shields.io/github/forks/zsqw123/Non-Lose-EditText)](https://github.com/zsqw123/Non-Lose-EditText/network)

## 功能

> 每一次编辑操作实时写入数据库，包括撤销栈和重做栈，即使退出软件撤销回退功能也不会重置

## 起源

> 在一天早上我写了500多字的笔记，笔记内容十分重要，我使用了小米便签，我知道它有自动保存功能，我也比较信任这个功能，但是一次意外的复制粘贴，我不小心将全文选中然后替换掉了，此时我点返回将输入键盘收起，却意外返回到上一层目录，再回去时，数据已经丢失，撤销栈也被清除。

在尝试了市面上很多编辑器，以及号称永不丢失的纯***，我发现他们都没有保存撤销栈的功能，用户一旦误操作，后果就是严重的数据丢失。  

很多软件把号称永不丢失这个功能作为实时保存字符串以及云同步，但他们忽视了容错性，手机莫名其妙结束一个不耗资源的编辑器的前台进程的可能性是很低的，而人出失误的概率是很大的。

实时保存用户的撤销栈和重做栈，才是提高用户容错率的必要解决方案。

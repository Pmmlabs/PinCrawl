# PinCrawl
A simple app to download any Pinterest user's pins to a local directory.

## Download
If you just want to use the app, download the JAR here: [Releases](../../releases/)

## Usage
Simply exectue the jar via command line including a user name argument.
The folder made with the pictures will be in your working directory (the one you exectued the command from)
> java -jar PinCrawl.jar kevinpauly

## Libraries download
This repo uses libraries as submodules, so before building sources yourself, run
```
git submodules init
git submodules update
```

## History
4/28/2016 - Vesion 1.2: Downloading info by JSON parsing

7/12/2015 - Vesion 1.1: Better board file naming

7/1/2015 - Version 1.0

## To Do
- Add arguments for overwrite, subtractive, and additive changes to existing directory structure
- Test on platforms other than Win x64
- GUI?

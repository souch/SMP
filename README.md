## SicMu Player for Android

Every songs of the phone are put in a unique big song list.

Songs are sorted and grouped by folders, genres, artists, albums and album's track.

Works on old slow small devices (from Android froyo 2.2).

No eye candy graphics.

![Folder list](screen4.7_folder.png)&nbsp;
![Artist list](screen3.5_artist.png)

[More Screenshots](Screenshots.md)


### detailed features

- play mp3, ogg, flac, midi, wav, mp4, 3gp... see android mediaplayer supported media formats (depends on android version).
- bluetooth support
- sorted by artists, albums and track number
- or sorted by folders, artists, albums and track number
- groups can be fold/unfold
- show current played song in list
- notification when playing
- seek bar
- shake the phone to go to next song
- disable/enable lockscreen
- on app startup, scroll to last song played


### Help

- tap on song to start it
- tap on the left of a group to play the first song it contains or unfolded the group.
- tap on the right of a group (on the v or ^ letter) to fold / unfold it
- press on home button or back button to shut the song list and keep the music playing in background
- press the button to the right of next to scroll the list to current played song
- open the padlock button to disable lock screen (it prevents locking only when the app is on top; do not work on some device)
- root folder can be customized: it is used only for hiding the first portion of the folders path when songs are sorted by folder


### Todo (perhaps :-)

- group by genre
- fast on big music list (> 5Go)
- guess "smart" track number (2 < 11, song file name vs id tag, ...)
- quick jump with letter in the right (like in contact)
- zoom make letter (and button) bigger or smaller
- speak the title ?
- cursor for volume ? ( half/full?)
- swipe to go to list of song <-> song details
- mp3 tag editor ?
- help + infobulle ?
- playlist?
- small vibration on action?
- pinned section (upper group level stay at top until another one appears)?
- shuffle
- long press on back button kill the app
- button to go to the last position after automatic scrolling
- option: relock on pause even if unlocked
- search?


### Installation

The SicMu Player is available on [F-Droid](https://f-droid.org/wiki/page/SicMu_Player)


### Credits

Lot's of time saved thanks to Sue Smith's [tutorials on creating a Music Player on Android](http://code.tutsplus.com/tutorials/create-a-music-player-on-android-project-setup--mobile-22764).

Use some icons done by Daniele De Santis (Creative Commons Attribution 3.0 Unported), found on iconfinder.com.

Seekbar style created by Jérôme Van Der Linden (Creative Commons Attribution 3.0 Unported), found on http://android-holo-colors.com.


### Developer

Compiled with Android Studio.
Robotium non regression tests are available in the androidTest folder.
Tested on Gingerbread (2.3.6), Nexus 4 JellyBean (4.1.2), Samsung S3 (4.3).

Detailed todo list in misc/TODO.txt

Feel free to add GitHub issues (feature request, bugs...).


### Donation

If you don't know what to do with your money or want to make me smile the whole day:
[donate](http://rodolphe.souchaud.free.fr/donate)


### License

SicMu Player is licensed under the GPLv3. See file LICENSE for more details.


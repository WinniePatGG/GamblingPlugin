Casino music - how to add your own tracks
=========================================

`MusicManager` (plugin) plays `gambling:menu_music` in the /casino menu and
`gambling:game_music` while the player is in a game. Both sound events are
defined in `assets/gambling/sounds.json`.

1. Convert your songs to Ogg Vorbis (.ogg - NOT mp3, NOT plain .ogg metadata
   with an mp3 stream inside, or you will hear silence):

       ffmpeg -i my_song.mp3 -c:a libvorbis -q:a 6 -ar 44100 menu_music.ogg

   (In Audacity: File > Export > "Ogg Vorbis Files".)

2. Drop the .ogg files next to this README:

       assets/gambling/sounds/menu_music.ogg    -> menu track (/casino)
       assets/gambling/sounds/game_music.ogg    -> in-game track

3. Point the plugin config (`plugins/GamblingPlugin/config.yml`) at them and set
   the real track length so the loop repeats seamlessly:

       music:
         menu:
           sound: gambling:menu_music
           length-seconds: 186
         game:
           sound: gambling:game_music
           length-seconds: 143

4. Rezip the pack and serve it (see repository README / server.properties).

Optional: a shuffled playlist - list multiple variants, the client picks one at
random each time the track (re)triggers:

    {
      "menu_music": {
        "sounds": [
          { "name": "gambling:menu_music", "stream": true },
          { "name": "gambling:menu_music_2", "stream": true, "weight": 2 }
        ]
      }
    }

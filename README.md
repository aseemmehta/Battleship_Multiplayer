# Battleship_Multiplayer

### To create the Game network
Only needs to be done once.
`docker network create --subnet=172.18.0.0/16 nodenet `

### Stpes to run the Server
1. sudo docker build -t javaapptest1 .
2. sudo docker run -it --cap-add=NET_ADMIN --net nodenet --ip 172.18.0.21 javaapptest1

### Stpes to run the Client (User's System)
1. sudo docker build -t javaapptest2 .
2. Player1: sudo docker run -it --cap-add=NET_ADMIN --net nodenet --ip 172.18.0.22 javaapptest2
3. Player2: sudo docker run -it --cap-add=NET_ADMIN --net nodenet --ip 172.18.0.23 javaapptest2

Follow the instructions in the Game

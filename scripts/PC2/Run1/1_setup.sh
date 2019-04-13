# GLOBAL INFORMATION
ACCESS_POINT="PeerRMI"

# PEER INFORMATION
VERSION_APP="1.0"
PEER_ID="1"

# CLIENT INFORMATION
APP_NAME="TestApp"

# SETUP
gnome-terminal -e "rmiregistry"
sleep 1

# RUN THE PEERS
for i in 2 3 4 5 6
do 
	gnome-terminal -e "./peer.sh $i"
done


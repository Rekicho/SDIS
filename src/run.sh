# GLOBAL INFORMATION
ACCESS_POINT="PeerRMI"

# SERVER INFORMATION
VERSION_APP="1.0"
SERVER_ID="1"

# CLIENT INFORMATION
APP_NAME="TestApp"

# USER SPECIFCATION
FROM_SERVER_ID=${1:-1}
TO_SERVER_ID=${2:-$FROM_SERVER_ID}


# SETUP
start rmiregistry
sleep 1

# RUN THE SERVERS
for i in $(seq $FROM_SERVER_ID $TO_SERVER_ID)
do 
	start ./peer.sh $i
done

start ./client.sh BACKUP test1.pdf 3
# start ./client.sh RESTORE test1.pdf
# start ./client.sh DELETE test1.pdf
# start ./client.sh RECLAIM 0
# start ./client.sh STATE

$SHELL
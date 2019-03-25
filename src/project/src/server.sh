# GLOBAL INFORMATION
ACCESS_POINT="ServerRMI"

# SERVER INFORMATION
VERSION_APP="1.0"

# IPs AND PORTs
MC_MULTICAST_IP="224.0.0.3"
MC_PORT="3333"

MDB_MULTICAST_IP="224.0.0.4"
MDB_PORT="4444"

MDR_MULTICAST_IP="224.0.0.5"
MDR_PORT="5555"

# GET ID OF THE SERVER
SERVER_ID=${1:-1}		# Default is set to 1, ${1:-n} to change default to n

java Server $VERSION_APP $SERVER_ID $ACCESS_POINT $MC_MULTICAST_IP $MC_PORT #$MDB_MULTICAST_IP $MDB_PORT $MDR_MULTICAST_IP $MDR_PORT
sleep 1

$SHELL
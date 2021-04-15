#!/usr/bin/env bash
trap cleanup 1 2 3 6

cleanup (){
  pkill -f 'python'
  pkill -f 'rmiregistry'
  #pkill -f 'java'
}

#compile
compile(){
  javac -d . ../server/*.java  ../client/*.java ../../util/rmisetup/*.java
}

#runpython
run_python(){
  konsole --noclose --new-tab -e bash _1_runpython.sh &
}

#rmiregisty
start_registry(){
 konsole --noclose --new-tab -e bash _2_runregistry.sh &
}

#server
start_server(){
  konsole --noclose --new-tab -e bash _3_runserver.sh &
}

start_client(){
  konsole --noclose --new-tab -e bash _4_runclient.sh
}


cleanup
#compile
sleep 1
run_python
sleep 2
start_registry
sleep 2
start_server
sleep 2
start_client

#sleep 4
#cleanup






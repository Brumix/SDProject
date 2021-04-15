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

#runProducer
run_producer(){
  konsole --noclose --new-tab -e bash runproducer.sh &
}

#rmiregisty
run_consumer(){
 konsole --noclose --new-tab -e bash runconsumer.sh
}



cleanup
#compile
sleep 1
run_producer
sleep 1
run_consumer

#sleep 4
#cleanup






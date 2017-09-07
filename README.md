# JAX-RS_Rx

Simple JAX-RS Client that use JAX-RS 2.1 Reactive Client API to chain 2 services invocations:
* Get the exposed IP adress from http://api.ipify.org/
* Based on the IP, get the location from https://ipvigilante.com

:warning: This sample focus just the CompltionStage aspect, it doesn't handle security (it'll trust any cert) nor deal with error!

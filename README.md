# GroupMessenger2
Implemented a group messenger, which enables an Android device to multicast messages to a group of devices.

There are two main parts in this project:

1)Ensuring Total and FIFO ordering of messages – Implemented ISIS algorithm. 

FIFO ordering – Message delivery order at each device should preserve the message sending order from each device.

Total ordering – Every device delivers messages in same order.

2)Failure handling – Ensured that ordering of messages is maintained even after a node fails by detecting the failed node and doing the necessary state clean up.

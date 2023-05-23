# Workshop
Thread pool with smart task queue manager.

Example usage: 

    Workshop ws = new Workshop(
                4, //Worker count (max simultaneous active thread count)
                10 //Max tasks per worker (thread)
        );
    //Adding task to queue    
    ws.execute(() -> {
       //Do something
    });

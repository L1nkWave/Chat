// Wait for the MongoDB replica set to become available
function waitForMongoDB(timeout) {

  print("Waiting for MongoDB to initialize...");

  var startTime = new Date();
  var status;
  while (true) {
    status = rs.status();
    if (status.ok) {
      print("MongoDB is initialized!");
      break;
    }

    if ((new Date()) - startTime > timeout) {
      print("Timeout waiting for MongoDB to initialize");
      break;
    }

    print("Waiting for MongoDB to initialize...");
    sleep(1000);
  }
}

// Initialize replica set
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "localhost:27017" }
  ]
});

// Wait for MongoDB to initialize
waitForMongoDB(5000); // Adjust timeout as needed

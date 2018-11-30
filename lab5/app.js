const fs = require('fs');
const file = require('file');
var async = require('async');



function countLines(file, cb){
    var count = 0;
    fs.createReadStream(file).on('data', function(chunk) {
        count += chunk.toString('utf8')
        .split(/\r\n|[\n\r\u0085\u2028\u2029]/g)
        .length-1;
    }).on('end', function() {
        console.log(file, count);
        cb(count);
    }).on('error', function(err) {
        console.error(err);
        cb(0);
    });
}


function main(dir_path, synctype){
    var sum = 0;
    var filesNumber = 0;
    var isEveryFileTraversed = 0;
    
    
    var start = new Date();
    function addSum(lines){
        sum += lines;
        isEveryFileTraversed--;
        if(isEveryFileTraversed === 0) {
            var stop = new Date();
            console.log();
            console.log("Sum: %d", sum);
            console.log("Time: %d", stop-start);
        }
    }
    
    // walks synchronusly, do sync
    if(synctype === "sync"){
        file.walkSync(dir_path, function(dirPath, dirs, files){
                      isEveryFileTraversed += files.length;
                      files.forEach(element => {countLines(dirPath+"/"+element, addSum)});
                      });
    }

    
    // walks asynchronusly, do async
    if(synctype === "async"){
        file.walk(dir_path, function(nulll, dirPath, dirs, files){
                  isEveryFileTraversed += files.length;
                  async.each(files, function(file){
                             countLines(file, addSum);
                             });
                  });
    }
    
    // walks sync, do async
    if(synctype === "sync-async"){
        file.walkSync(dir_path, function(dirPath, dirs, files){
                  isEveryFileTraversed += files.length;
                  async.each(files, function(file){
                             countLines(dirPath+"/"+file, addSum);
                             });
                  });
    }

    // walks asynchronusly, do sync
    if(synctype === "async-sync"){
       file.walk(dir_path, function(nulll, dirPath, dirs, files){
              isEveryFileTraversed += files.length;
              files.forEach(element => {countLines(element, addSum)});
              });
    }
}

main("./PAM08", "async-sync");

import caffe
import numpy as np
import sys

if len(sys.argv) != 3:
    print "Usage: python binaryproto_to_msg.py meanfile.mean out.msg"
    sys.exit()

blob = caffe.proto.caffe_pb2.BlobProto()
data = open( sys.argv[1] , 'rb' ).read()
blob.ParseFromString(data)
arr = np.array( caffe.io.blobproto_to_array(blob) )
out = arr[0]
np.save( sys.argv[2] , out )

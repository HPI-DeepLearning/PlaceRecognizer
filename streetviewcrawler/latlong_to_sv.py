
from models import Pano
from google_streetview_api import *
from time import sleep

import grequests
import requests
import json
import csv, time, cStringIO
import math
import wget

if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument('csv_path', help='CSV file with header that contains lat-long coordinates')
    parser.add_argument('-d', '--db', default='mongodb://localhost/test', help='mongodb URI')
    parser.add_argument('-b', '--batch-size', type=int, default=1, help='size of each request batch')
    parser.add_argument('-k', '--key', help='Google StreetView API key')
    parser.add_argument('-n', '--dry-run', action='store_true', help='dry run')
    parser.add_argument('--lat', default='lat', help='header for latitude')
    parser.add_argument('--long', default='long', help='header for longitude')

    args = parser.parse_args()

    #if not args.dry_run:
        #me.connect('', host=args.db)

    exp_backoff = 1.
    has_error = False
    imgId = 0
    with open(args.csv_path) as f:
        r = csv.DictReader(f)
        longlats = set()
        params = []
        t0 = time.time()
        t1 = t0
        locId = 0
        for i, row in enumerate(r):
            if i % 20 == 0:
                t2 = time.time()
                print '%d done, elapsed %fs' % (i, t2-t1)
                t2 = t1

            loc = row[args.long], row[args.lat]
            if loc not in longlats:
                try:
                    floc = (float(row[args.long]), float(row[args.lat]))
                except:
                    continue

                fov = int(row['fov'])
                offsets = [-30, -25, -20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30]
                step = 1
                if float(row["starthead"]) != 0 and row['endhead'] != 0:
                    x = int(float(row["endhead"])-float(row["starthead"]))
                else:
                    x = fov/2
                    if x < 10:
                        step = x/20
                        x = 20
                for headings in range(0, x):
                    headingOffset = (headings - x/2)*step#((2*headings - x/2) * fov/24)
                    meta_url = generate_meta_url(floc, fov=fov, key=args.key) 
                    meta_content = json.loads(requests.get(meta_url).text)

                    if meta_content["status"] != 'OK':
                        has_error = True

                    lat2 = math.radians(float(row['buildingLat']))
                    long2 = math.radians(float(row['buildingLong']))
                    lat1 = math.radians(float(meta_content["location"]["lat"]))#float(row[args.lat]))
                    long1 = math.radians(float(meta_content["location"]["lng"]))

                    dLong = long1 - long2
                    y = math.sin(dLong) * math.cos(lat2)
                    x = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(dLong)
                    bearing = math.atan2(y, x)

                    bearing = math.degrees(bearing)
                    bearing = (bearing + 360) % 360
                    bearing = 360 - bearing + headingOffset

                    if float(row["starthead"]) != 0 and float(row['endhead']) != 0:
                        bearing = float(row["starthead"]) + headings

                    params.append(((float(meta_content["location"]["lng"]), float(meta_content["location"]["lat"])), bearing))
                    l = len(params)
                    # build URL
                    if l >= args.batch_size:
                        urls = [generate_pano_url(floc, fov=fov, heading=heading, pitch=float(row["pitch"]), key=args.key) for floc, heading in params]
                        
                        reqs = (grequests.get(url) for url in urls)
                        # send requests and block
                        reps = grequests.map(reqs)

                        for j, (param, rep) in enumerate(zip(params, reps)):
                            if rep.status_code == 200:
                                f = open(row['name'] + "_" + str(locId) + "-" + str(imgId) + ".png", 'w')
                                f.write(rep.content)
                                f.close()
                                imgId = imgId + 1
                            else:
                                print 'error getting %r from %s' % (param, rep.url)
                                params.append(param)
                                has_error = True

                        if has_error:
                            exp_backoff *= 2
                        else:
                            exp_backoff = 0

                        params = params[l:]
                        sleep(exp_backoff)
                
                longlats.add(loc)
                locId = locId + 1

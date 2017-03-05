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
    parser.add_argument('-b', '--batch-size', type=int, default=1, help='size of each request batch')
    parser.add_argument('-k', '--key', help='Google StreetView API key')

    args = parser.parse_args()

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
                    headingOffset = (headings - x/2)*step
                    meta_url = generate_meta_url(floc, fov=fov, key=args.key)
                    meta_content = json.loads(requests.get(meta_url).text)

                    if meta_content["status"] != 'OK':
                        has_error = True

                    lat2 = math.radians(float(row['buildingLat']))
                    long2 = math.radians(float(row['buildingLong']))
                    lat1 = math.radians(float(meta_content["location"]["lat"]))
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
                                fname = row['name'] + "_" + str(locId) + "-" + str(imgId)
                                f = open(fname + ".png", 'w')
                                f.write(rep.content)
                                f.close()

                                from PIL import Image
                                from resizeimage import resizeimage

                                with open(fname + '.png', 'r+b') as f:
                                    with Image.open(f) as image:
                                        cover = resizeimage.resize_cover(image, [image.width * 0.9, image.height * 0.9])
                                        cover.save(fname + '_medium.png', image.format)

                                with open(fname + '.png', 'r+b') as f:
                                    with Image.open(f) as image:
                                        cover = resizeimage.resize_cover(image, [image.width * 0.8, image.height * 0.8])
                                        cover.save(fname + '_small.png', image.format)

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

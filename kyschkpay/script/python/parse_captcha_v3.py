import sys
import os
import re
import subprocess
import tempfile
import pytesseract
import base64
from PIL import Image
from io import BytesIO

tessaract_path = ''

def parse_captcha(filename):
    """Return the text for thie image using Tesseract
    """
    img = threshold(filename)
    return tesseract(img)


def threshold(imgBase64, limit=80):
    """Make text more clear by thresholding all pixels above / below this limit to white / black
    """
    
    # read in colour channels
    img = Image.open(BytesIO(base64.b64decode(imgBase64)))
    
    # resize to make more clearer
    m = 1.5
    img = img.resize((int(img.size[0]*m), int(img.size[1]*m))).convert('RGBA')
    pixdata = img.load()

    for y in range(img.size[1]):
        for x in range(img.size[0]):
            if pixdata[x, y][0] < limit:
                # make dark color black
                pixdata[x, y] = (0, 0, 0, 255)
            else:
                # make light color white
                pixdata[x, y] = (255, 255, 255, 255)

    return img.convert('L') # convert image to single channel greyscale


def call_command(*args):
    """call given command arguments, raise exception if error, and return output
    """
    
    c = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    output, error = c.communicate()
    if c.returncode != 0:
        if error:
            print (error)
        print ("Error running `%s'" % ' '.join(args))
    return output


def tesseract(image):
    """Decode image with Tesseract  
    """
    # create temporary file for tiff image required as input to tesseract
    input_file = tempfile.NamedTemporaryFile(suffix='.tif', dir='.', delete=False)
    image.save(input_file.name)

    # perform OCR
    output_filename = input_file.name.replace('.tif', '.txt')
    #call_command(tessaract_path + 'tesseract', input_file.name, output_filename.replace('.txt', ''))
    
    text = pytesseract.image_to_string(Image.open(input_file))
    
    # read in result from output file
    #result = open(output_filename).read()
    #os.remove(output_filename)

    # close and remove input_file
    input_file.close();
    os.remove(input_file.name)
    
    return clean(text)


def clean(s):
    """Standardize the OCR output
    """
    # remove non-alpha numeric text
    return re.sub('[\W]', '', s)



if __name__ == '__main__':
    imgBase64 = sys.argv[1]
    tessaract_path = sys.argv[2]

    if imgBase64:
        img = threshold(imgBase64)
        print ('captcha_txt:',tesseract(img))
    else:
        print ('Usage: %s [image1] [image2] ...' % sys.argv[0])

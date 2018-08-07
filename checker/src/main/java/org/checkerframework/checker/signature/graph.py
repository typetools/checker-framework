import sys, os, re
import subprocess as sb

class Node:
    def __init__(self):
        self.Ends = list()

def main():
    superTypes = dict()
    for f in sys.argv[1:]:
        if not os.path.isfile(f):
            continue
        nm = f.replace('.java', '')
        contents = list()
        for l in open(f):
            l = l.strip()
            if len(contents) > 0:
                if re.fullmatch('.*@.*', l):
                    break
                contents.append(l)
                continue
            if re.fullmatch('@SubtypeOf.*', l):
                contents.append(l)
                continue
        m = re.findall(r'^@SubtypeOf\s*\(\s*{?(.*)}?\s*\)\s*$',' '.join(contents))
        if len(m) < 1:
            continue
        else:
            m = m[0].split()
            m = [ f.replace('.class', '') for f in m ]
            m = [ ''.join([c for c in s if c.isalnum()]) for s in m ]
            m = [ s for s in m if s != '' ]
        superTypes[nm] = m
    if os.path.exists('graph.dot'):
        os.remove('graph.dot')
    f = open('graph.dot', 'w')
    f.write('digraph G {\n')
    for n in superTypes:
        for t in superTypes[n]:
            f.write(n+' -> '+t+'\n')
    if os.path.isfile('newAnnotations'):
        for l in open('newAnnotations'):
            if l.strip() != '':
                f.write(l.strip()+' [color=red]\n')
    f.write('}')
    f.close()
    sb.call(['dot', '-Tpdf', 'graph.dot', '-o', 'graph.pdf'])



if __name__ == '__main__':
    main()
# __magic__

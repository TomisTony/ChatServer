import time
import subprocess
import multiprocessing


def action0():
    proc = subprocess.Popen(['nc', 'localhost', '4657'],
                            stdin=subprocess.PIPE,
                            stderr=subprocess.PIPE)
    begin = time.time()
    while(time.time()-begin < 2): pass
    proc.stdin.write(b"1\n")
    begin = time.time()
    while(time.time()-begin < 5): pass
    proc.stdin.write(b"END\n")
    begin = time.time()
    while(time.time()-begin < 2): pass

# def action1():
#     proc = subprocess.Popen(['nc', 'localhost', '4657'],
#                             stdin=subprocess.PIPE,
#                             stderr=subprocess.PIPE)
#     begin = time.time()
#     while(time.time()-begin < 3): pass
#     proc.stdin.write(b"END\n")


if __name__ == "__main__":
    for i in range(10):
        multiprocessing.Process(target=action0).start()
    
    # for i in range(5):
    #     multiprocessing.Process(target=action1).start()

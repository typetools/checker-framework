// Test case for Issue 548:
// https://github.com/typetools/checker-framework/issues/548

class TryFinallyBreak {
    String testWhile1() {
        String ans = "x";
        while (this.hashCode() > 10000) {
            try {
                // empty body
            } finally {
                ans = null;
            }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testWhile2() {
        String ans = "x";
        while (true) {
            try {
                // Note the additional break;
                break;
            } finally {
                ans = null;
            }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testWhile3() {
        String ans = "x";
        while (true) {
            try {
                testWhile3();
            } catch (Exception e) {
                break;
            } finally {
                ans = null;
            }
            ans = "x";
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testWhile4() {
        String ans = "x";
        while (true) {
            if (true) {
                try {
                    break;
                } finally {
                    ans = null;
                    break;
                }
            }
            ans = "x";
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testWhile5() {
        String ans = "x";
        while (true) {
            while (true) {
                try {
                    // Note the additional break;
                    break;
                } finally {
                    ans = null;
                }
            }
            ans = "x";
            break;
        }
        return ans;
    }

    String testWhile6(boolean cond) {
        String ans = "x";
        OUTER:
        while (cond) {
            while (cond) {
                try {
                    if (cond) {
                        break OUTER;
                    }
                } finally {
                    ans = null;
                }
            }
            ans = "x";
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testWhile7(boolean cond) {
        String ans = "x";
        OUTER:
        while (cond) {
            try {
                while (cond) {
                    try {
                        if (cond) {
                            break OUTER;
                        }
                    } finally {
                        ans = null;
                    }
                }
            } finally {
                ans = "x";
            }
        }
        return ans;
    }

    String testWhile8(boolean cond) {
        String ans = "x";
        OUTER:
        while (cond) {
            try {
                while (cond) {
                    try {
                        if (cond) {
                            break OUTER;
                        }
                    } finally {
                        ans = "x";
                    }
                }
            } finally {
                ans = null;
            }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testDoWhile1() {
        String ans = "x";
        do {
            try {
                // empty body
            } finally {
                ans = null;
            }
        } while (this.hashCode() > 10000);
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testDoWhile2() {
        String ans = "x";
        do {
            try {
                // Note the additional break;
                break;
            } finally {
                ans = null;
            }
        } while (true);
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testDoWhile3() {
        String ans = "x";
        do {
            try {
                testWhile3();
            } catch (Exception e) {
                break;
            } finally {
                ans = null;
            }
            ans = "x";
        } while (true);
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testDoWhile4() {
        String ans = "x";
        do {
            if (true) {
                try {
                    break;
                } finally {
                    ans = null;
                    break;
                }
            }
            ans = "x";
        } while (true);
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testDoWhile5() {
        String ans = "x";
        do {
            do {
                try {
                    // Note the additional break;
                    break;
                } finally {
                    ans = null;
                }
            } while (true);
            ans = "x";
            break;
        } while (true);
        return ans;
    }

    String testDoWhile6(boolean cond) {
        String ans = "x";
        OUTER:
        do {
            do {
                try {
                    if (cond) {
                        break OUTER;
                    }
                } finally {
                    ans = null;
                }
            } while (cond);
            ans = "x";
        } while (cond);
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testDoWhile7(boolean cond) {
        String ans = "x";
        OUTER:
        do {
            try {
                do {
                    try {
                        if (cond) {
                            break OUTER;
                        }
                    } finally {
                        ans = null;
                    }
                } while (cond);
            } finally {
                ans = "x";
            }
        } while (cond);
        return ans;
    }

    String testDoWhile8(boolean cond) {
        String ans = "x";
        OUTER:
        do {
            try {
                do {
                    try {
                        if (cond) {
                            break OUTER;
                        }
                    } finally {
                        ans = "x";
                    }
                } while (cond);
            } finally {
                ans = null;
            }
        } while (cond);
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testFor1() {
        String ans = "x";
        for (; this.hashCode() > 10000; ) {
            try {
                // empty body
            } finally {
                ans = null;
            }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testFor2() {
        String ans = "x";
        for (; ; ) {
            try {
                // Note the additional break;
                break;
            } finally {
                ans = null;
            }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testFor3() {
        String ans = "x";
        for (; ; ) {
            try {
                testFor3();
            } catch (Exception e) {
                break;
            } finally {
                ans = null;
            }
            ans = "x";
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testFor4() {
        String ans = "x";
        for (; ; ) {
            if (true) {
                try {
                    break;
                } finally {
                    ans = null;
                    break;
                }
            }
            ans = "x";
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testFor5() {
        String ans = "x";
        for (; ; ) {
            for (; ; ) {
                try {
                    // Note the additional break;
                    break;
                } finally {
                    ans = null;
                }
            }
            ans = "x";
            break;
        }
        return ans;
    }

    String testFor6(boolean cond) {
        String ans = "x";
        OUTER:
        for (; ; ) {
            for (; cond; ) {
                try {
                    if (cond) {
                        break OUTER;
                    }
                } finally {
                    ans = null;
                }
            }
            ans = "x";
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testFor7(boolean cond) {
        String ans = "x";
        OUTER:
        for (; ; ) {
            try {
                for (; ; ) {
                    try {
                        if (cond) {
                            break OUTER;
                        }
                    } finally {
                        ans = null;
                    }
                }
            } finally {
                ans = "x";
            }
        }
        return ans;
    }

    String testFor8(boolean cond) {
        String ans = "x";
        OUTER:
        for (; ; ) {
            try {
                for (; ; ) {
                    try {
                        if (cond) {
                            break OUTER;
                        }
                    } finally {
                        ans = "x";
                    }
                }
            } finally {
                ans = null;
            }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testIf1() {
        String ans = "x";
        IF:
        if (true) {
            try {
                break IF;
            } finally {
                ans = null;
            }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testIf2(boolean cond) {
        String ans = "x";
        IF:
        if (cond) {
            if (cond) {
                try {
                    if (cond) {
                        break IF;
                    }
                } finally {
                    ans = null;
                }
            }
            ans = "x";
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testIf3(boolean cond) {
        String ans = "x";
        IF:
        if (cond) {
            try {
                if (cond) {
                    try {
                        if (cond) {
                            break IF;
                        }
                    } finally {
                        ans = null;
                    }
                }
            } finally {
                ans = "x";
            }
        }
        return ans;
    }

    String testIf4(boolean cond) {
        String ans = "x";
        IF:
        if (cond) {
            try {
                if (cond) {
                    try {
                        if (cond) {
                            break IF;
                        }
                    } finally {
                        ans = "x";
                    }
                }
            } finally {
                ans = null;
            }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testSwitch1() {
        String ans = "x";
        switch (ans) {
            case "x":
                try {
                    break;
                } finally {
                    ans = null;
                }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testSwitch2(boolean cond) {
        String ans = "x";
        SWITCH:
        switch (ans) {
            case "x":
                switch (ans) {
                    case "x":
                        try {
                            break SWITCH;
                        } finally {
                            ans = null;
                        }
                }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }

    String testSwitch3(boolean cond) {
        String ans = "x";
        SWITCH:
        switch (ans) {
            case "x":
                try {
                    switch (ans) {
                        case "x":
                            try {
                                break SWITCH;
                            } finally {
                                ans = null;
                            }
                    }
                } finally {
                    ans = "x";
                }
        }
        return ans;
    }

    String testSwitch4(boolean cond) {
        String ans = "x";
        SWITCH:
        switch (ans) {
            case "x":
                try {
                    switch (ans) {
                        case "x":
                            try {
                                break SWITCH;
                            } finally {
                                ans = "x";
                            }
                    }
                } finally {
                    ans = null;
                }
        }
        // :: error: (return.type.incompatible)
        return ans;
    }
}

#
#  Copyright 2015 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

##########################
# sayHello.pl
##########################

use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';
use ElectricCommander;
use ElectricCommander::PropDB;

$| = 1;

# The configuration parameter
my $configName = q{$[config]};

# Create ElectricCommander instance
my $ec = new ElectricCommander();
$ec->abortOnError(0);

# Get the configuration values
my $cfg  = new ElectricCommander::PropDB( $ec, "/myProject/helloworld_cfgs");
my %configValues = $cfg->getRow($configName);

# Check if configuration exists
unless ( keys(%configValues) ) {
    print "Configuration '[$configName]' does not exist\n";
    exit 1;
}

my $formalLevel = $configValues{'formalLevel'};

my $msg = 'say hello world!';

# Set the message in the job summary as well as print it in the step logs.
$ec->setProperty("summary", $msg . "\n");
print $msg;

exit(0);
